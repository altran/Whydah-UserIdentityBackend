package net.whydah.identity.util;

import java.io.File;
import java.io.IOException;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
	
	String currentDirectoryLockId;
	public static Map<String, Directory> dirs = Collections.synchronizedMap(new HashMap<String, Directory>());
	public static Map<String, IndexWriter> indexWriters = Collections.synchronizedMap(new HashMap<String, IndexWriter>());
	public Map<String, List<T>> addActionQueue = Collections.synchronizedMap(new HashMap<String, List<T>>());; 
	public Map<String, List<T>> updateActionQueue = Collections.synchronizedMap(new HashMap<String, List<T>>());;
	public Map<String, List<String>> deleteActionQueue = Collections.synchronizedMap(new HashMap<String, List<String>>());;
	ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

	public BaseLuceneIndexer(Directory luceneDirectory) throws IOException {
		currentDirectoryLockId = luceneDirectory.getLockID();
		if(!dirs.containsKey(currentDirectoryLockId)) {
			dirs.put(currentDirectoryLockId, luceneDirectory);
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
		return getIndexWriter().numDocs();
	}


	/**
	 * @return the indexWriter
	 */
	public synchronized IndexWriter getIndexWriter() {
		
		if(!indexWriters.containsKey(currentDirectoryLockId)) {
			try {
				IndexWriter indexWriter = createWriter(dirs.get(currentDirectoryLockId));
				if(indexWriter!=null) {
					indexWriters.put(currentDirectoryLockId, indexWriter);
				}				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return indexWriters.get(currentDirectoryLockId);
	}


	public void closeIndexWriter() {
		
		scheduledThreadPool.shutdown();
		
		closeIndexWriter(currentDirectoryLockId);
		
		getAddActionQueue().clear();
		getUpdateActionQueue().clear();
		getDeleteActionQueue().clear();
		
	}
	
	private static void closeIndexWriter(String dirId) {
		try {		
			if(indexWriters.get(dirId)!=null) {
				
				while(indexWriters.get(dirId).hasPendingMerges()) {
					Thread.sleep(100);
				}			
				indexWriters.get(dirId).close();
			}
			indexWriters.remove(dirId);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void closeAllIndexWriters() {
		for(String dirId : new ArrayList<>(dirs.keySet())) {
			closeIndexWriter(dirId);
		}
	}
	
	public static IndexWriter createWriter(Directory directory) throws IOException {
		
		if(IndexWriter.isLocked(directory)) {
			//IndexWriter.unlock(directory);
			return null;
		}
		
		if (directory instanceof RAMDirectory) {			
			return new IndexWriter(directory, new IndexWriterConfig(LUCENE_VERSION, ANALYZER));
		} else if (directory instanceof FSDirectory) {
			try {
//				File path = new File(((FSDirectory) directory).getDirectory().getPath());
//				if (!path.exists()) {
//					path.mkdir();
//				}
						
				IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
				indexWriterConfig.setMaxBufferedDocs(500);
				indexWriterConfig.setRAMBufferSizeMB(300);
				
				return new IndexWriter(directory, indexWriterConfig);
				
			} catch (IOException e) {
				e.printStackTrace();
				throw new IOException("Unable to access lock to lucene index worker for directory " + directory.toString());
			}		
		} else {
			throw new IOException("Directory type " + directory.getClass() + " is not supported");
		}

	}
}
