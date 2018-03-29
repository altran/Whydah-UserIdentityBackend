package net.whydah.identity.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.SegmentInfos.FindSegmentsFile;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import net.whydah.identity.user.search.LuceneUserIndexerImpl;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserIdentity;

public abstract class BaseLuceneIndexer<T> {

	public static final Version LUCENE_VERSION = Version.LUCENE_4_10_4;
	protected static final Analyzer ANALYZER = new StandardAnalyzer();

	protected final Logger log = LoggerFactory.getLogger(BaseLuceneIndexer.class);


	protected FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);


	protected boolean isQueueProcessing = false;

	Directory directory;
	String indexPath;
	String currentDirectoryLockId;

	
	public static Map<String, IndexWriter> indexWriters = Collections.synchronizedMap(new HashMap<String, IndexWriter>());
	
	//we use these queue to prevent loss of data in case there is some exception related to disk/memory
	public Map<String, List<T>> addActionQueue = Collections.synchronizedMap(new HashMap<String, List<T>>());
	public Map<String, List<T>> updateActionQueue = Collections.synchronizedMap(new HashMap<String, List<T>>());
	public Map<String, List<String>> deleteActionQueue = Collections.synchronizedMap(new HashMap<String, List<String>>());
	//this scheduled service will check and handle items in the queues
	ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

	public BaseLuceneIndexer(Directory luceneDirectory) throws IOException {
		this.directory = luceneDirectory;
		this.currentDirectoryLockId = luceneDirectory.getLockID();	
		if (directory instanceof FSDirectory) {	
			indexPath = ((FSDirectory) directory).getDirectory().getPath();
			File path = new File(indexPath);
			if (!path.exists()) {
				path.mkdir();
			}
		}

		addActionQueue.put(currentDirectoryLockId, Collections.synchronizedList(new ArrayList<>()));
		updateActionQueue.put(currentDirectoryLockId, Collections.synchronizedList(new ArrayList<>()));
		deleteActionQueue.put(currentDirectoryLockId, Collections.synchronizedList(new ArrayList<>()));

		ftNotTokenized.setTokenized(false);
		ftNotTokenized.setIndexed(true);
		ftTokenized.setTokenized(true);
		ftNotIndexed.setIndexed(false);

		checkQueueProcessWorker();
	}

	public boolean addToIndex(T obj) {

		try {

			Document doc = createLuceneDocument(obj);
			getIndexWriter().addDocument(doc);
			getIndexWriter().commit();
			return true;

		}
		catch(IllegalArgumentException e){
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("addToIndex failed for {}.", obj.toString(), e);
			getAddActionQueue().add(obj);
		}
		return false;
	}


	public void addToIndex(List<T> objs) {
		try {
			List<T> failures = new ArrayList<>();
			for (T obj : objs) {
				try {
					Document doc = createLuceneDocument(obj);
					getIndexWriter().addDocument(doc);
				} catch(IllegalArgumentException e){
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
					log.error("addToIndex failed for {}. This item was not added to lucene index.", obj.toString(), e);					
					failures.add(obj);
				}
			}
			//add back to queue
			for (T obj : failures) {
				getAddActionQueue().add(obj);
			}
			//commit anyway
			getIndexWriter().commit();

		}  catch (Exception e) {
			e.printStackTrace();
			//add back to queue because the whole commit process failed
			for (T obj : objs) {
				getAddActionQueue().add(obj);
			}
		}


	}


	public boolean updateIndex(T obj) {
		try {
			getIndexWriter().updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
			getIndexWriter().commit();
			return true;
		} catch(IllegalArgumentException e){
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("updating {} failed.", obj.toString(), e);
			getUpdateActionQueue().add(obj);
		}
		return false;
	}

	public void updateIndex(List<T> objs) {
		try {

			List<T> failures = new ArrayList<>();
			for (T obj : objs) {
				try {
					getIndexWriter().updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
				} catch(IllegalArgumentException e){
					e.printStackTrace();
				} catch (Exception e) {
					log.error("updating {} failed.", obj.toString(), e);		
					failures.add(obj);
				}
			}
			//add back to queue
			for (T obj : failures) {
				getUpdateActionQueue().add(obj);
			}
			//commit anyway
			getIndexWriter().commit();

		}  catch (Exception e) {
			e.printStackTrace();
			//add back to queue because the whole commit process failed
			for (T obj : objs) {
				getUpdateActionQueue().add(obj);
			}
		}

	}



	public boolean removeFromIndex(String id) {

		try {
			getIndexWriter().deleteDocuments(getTermForDeletion(id));
			getIndexWriter().commit();
			return true;
		} catch(IllegalArgumentException e){
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("removeFromIndex failed. uid={}", id, e);
			getDeleteActionQueue().add(id);
		}
		return false;
	}

	protected abstract Term getTermForDeletion(String id);

	protected abstract Term getTermForUpdate(T obj);

	public void removeFromIndex(List<String> ids) {
		try {
			List<String> failures = new ArrayList<>();
			for (String id : ids) {
				try {
					getIndexWriter().deleteDocuments(getTermForDeletion(id));
				} catch(IllegalArgumentException e){
					e.printStackTrace();
				} catch (Exception e) {
					log.error("updating {} failed.", id, e);		
					failures.add(id);
				}
			}
			//add back to queue
			for (String uid : failures) {
				getDeleteActionQueue().add(uid);
			}
			//commit anyway
			getIndexWriter().commit();
		} catch (Exception e) {
			e.printStackTrace();
			//add back to queue because the whole commit process failed
			for (String id : ids) {
				getDeleteActionQueue().add(id);
			}
		} 
	}

	protected abstract Document createLuceneDocument(T obj);



	public void handleAddIndexFromQueue() {
		//adding one by one can be a costly process, so we may use bulk insert
		//		for (Iterator<T> iterator = getAddActionQueue().iterator(); iterator.hasNext(); ) {
		//			T value = iterator.next();
		//			if(addToIndex(value)) {
		//				iterator.remove();
		//			}
		//		}

		//bulk insert
		if(getAddActionQueue().size()>0) {

			//List<UserIdentity> clones = new ArrayList<UserIdentity>(Collections.unmodifiableCollection(getAddActionQueue()));//get all items from the queue
			List<T> subList = getAddActionQueue().subList(0, getAddActionQueue().size());
			List<T> clones = new ArrayList<T>(subList);
			subList.clear();
			addToIndex(clones);

		}
	}
	public void handleUpdateIndexFromQueue() {
		//		for (Iterator<T> iterator = getUpdateActionQueue().iterator(); iterator.hasNext(); ) {
		//			T value = iterator.next();
		//			if(updateIndex(value)) {
		//				iterator.remove();
		//			}
		//		}

		//bulk update
		if(getUpdateActionQueue().size()>0) {
			List<T> subList = getUpdateActionQueue().subList(0, getUpdateActionQueue().size());
			List<T> clones = new ArrayList<T>(subList);
			subList.clear();
			updateIndex(clones);
		}
	}
	public void handleRemoveIndexFromQueue() {
		//		for (Iterator<String> iterator = getDeleteActionQueue().iterator(); iterator.hasNext(); ) {
		//			String value = iterator.next();
		//			if(removeFromIndex(value)) {
		//				iterator.remove();
		//			}
		//		}

		//bulk delete
		if(getDeleteActionQueue().size()>0) {
			List<String> subList = getDeleteActionQueue().subList(0, getDeleteActionQueue().size());
			List<String> clones = new ArrayList<String>(subList);
			subList.clear();
			removeFromIndex(clones);
		}
	}


	public void checkQueueProcessWorker() {	

		log.debug("startProcessWorker - Current Time = " + new Date());
		scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				if(!isQueueProcessing()) {
					setQueueProcessing(true);
					handleRemoveIndexFromQueue();
					handleUpdateIndexFromQueue();
					handleAddIndexFromQueue();
					setQueueProcessing(false);
				}
			}
		}, 5, 5, TimeUnit.SECONDS);


	}


	public List<T> getAddActionQueue(){
		return addActionQueue.get(currentDirectoryLockId);
	}

	public List<T> getUpdateActionQueue(){
		return updateActionQueue.get(currentDirectoryLockId);
	}

	public List<String> getDeleteActionQueue(){
		return deleteActionQueue.get(currentDirectoryLockId);
	}

	/**
	 * @return the isQueueProcessing
	 */
	public boolean isQueueProcessing() {
		return isQueueProcessing;
	}


	/**
	 * @param isQueueProcessing the isQueueProcessing to set
	 */
	public void setQueueProcessing(boolean isQueueProcessing) {
		this.isQueueProcessing = isQueueProcessing;
	}

	public int numDocs() {
		try {
			return getIndexWriter().numDocs();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return 0;
	}


	/**
	 * @return the indexWriter
	 */

	/** 
	 * Gets an index writer for the repository. The index will be created if it does not already exist or if forceCreate is specified.
	 * @param repository
	 * @return an IndexWriter
	 * @throws IOException
	 */

	public synchronized IndexWriter getIndexWriter() throws IOException {
		IndexWriter writer = indexWriters.get(currentDirectoryLockId);

		if(writer ==null) {
			if (directory instanceof FSDirectory) {
				try {
					((FSDirectory) directory).getDirectory();	//this will call ensureOpen();			
				}catch(AlreadyClosedException ex) {
					//should reopen
					directory = FSDirectory.open(new File(indexPath));
				}
			}

			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
			indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			indexWriterConfig.setMaxBufferedDocs(500);
			indexWriterConfig.setRAMBufferSizeMB(300);
			try {
				if (IndexWriter.isLocked(directory)) {
			        IndexWriter.unlock(directory);
			        log.info("Removed Lucene lock file in " + directory);
			      }
				writer = new IndexWriter(directory, indexWriterConfig);
				indexWriters.put(currentDirectoryLockId, writer);
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new IOException("Unable to access lock to lucene index worker for directory " + directory.toString());
			}	

		}

		return writer;


	}


	public void closeIndexWriter() {

		while(getAddActionQueue().size()>0 || 
				getUpdateActionQueue().size()>0 ||
				getDeleteActionQueue().size()>0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}

		scheduledThreadPool.shutdown();		
		closeIndexWriter(currentDirectoryLockId);	
	}

	private static void closeIndexWriter(String dirId) {
		try {		
			if(indexWriters.get(dirId)!=null) {				
				IndexWriter writer = indexWriters.remove(dirId);
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void closeAllIndexWriters() {

		for(String dirId : new ArrayList<>(indexWriters.keySet())) {
			closeIndexWriter(dirId);
		}
	}

}
