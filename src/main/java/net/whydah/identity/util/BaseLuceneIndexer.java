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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseLuceneIndexer<T> {

	public static final Version LUCENE_VERSION = Version.LUCENE_4_10_4;
	protected static final Analyzer ANALYZER = new StandardAnalyzer();

	protected final Logger log = LoggerFactory.getLogger(BaseLuceneIndexer.class);


	protected FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);

	Directory directory;
	String indexPath;
	
	public BaseLuceneIndexer(Directory luceneDirectory) throws IOException {
		this.directory = luceneDirectory;
		
		if (directory instanceof FSDirectory) {	
			indexPath = ((FSDirectory) directory).getDirectory().getPath();
			File path = new File(indexPath);
			if (!path.exists()) {
				path.mkdir();
			}
		}
		
		ftNotTokenized.setTokenized(false);
		ftNotTokenized.setIndexed(true);
		ftTokenized.setTokenized(true);
		ftNotIndexed.setIndexed(false);
	}

	public synchronized boolean addToIndex(T obj) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			Document doc = createLuceneDocument(obj);
			w.addDocument(doc);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("addToIndex failed for {}, error: {}", obj.toString(), e.getMessage());
		} finally {
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		return false;
	}


	public synchronized void addToIndex(List<T> objs) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			
			for (T obj : objs) {
				try {				
					Document doc = createLuceneDocument(obj);
					w.addDocument(doc);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("addToIndex failed for {}. This item was not added to lucene index.", obj.toString(), e);					
				}
			}

		}  catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}


	}


	public synchronized boolean updateIndex(T obj) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			w.updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
			return true;
		}  catch (Exception e) {
			e.printStackTrace();
			log.error("updateIndex {} failed. Error {}", obj.toString(), e);
			
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		return false;
	}

	public synchronized void updateIndex(List<T> objs) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			for (T obj : objs) {
				try {
					w.updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
				} catch(IllegalArgumentException e){
					e.printStackTrace();
				} catch (Exception e) {
					log.error("updateIndex {} failed.", obj.toString(), e);		
					
				}
			}

		}  catch (Exception e) {
			e.printStackTrace();
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}

	}



	public synchronized boolean removeFromIndex(String id) {

		IndexWriter w = null;
		try {
			w = getIndexWriter();
			w.deleteDocuments(getTermForDeletion(id));		
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("removeFromIndex failed. uid={}. Error {}", id, e);
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}

		return false;
	}

	protected abstract Term getTermForDeletion(String id);

	protected abstract Term getTermForUpdate(T obj);

	public synchronized void removeFromIndex(List<String> ids) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			for (String id : ids) {
				try {
					w.deleteDocuments(getTermForDeletion(id));
				}catch (Exception e) {
					log.error("removeFromIndex {} failed.", id, e);						
				}
			}
		
		} catch (Exception e) {
			e.printStackTrace();			
		}  finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}

	}

	protected abstract Document createLuceneDocument(T obj);



	public synchronized int numDocs() {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			return w.numDocs();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		return 0;
	}

	public boolean isDirectoryOpen() {
		if (directory instanceof FSDirectory) {
			try {
				((FSDirectory) directory).getDirectory();	//this will call ensureOpen();			
			}catch(AlreadyClosedException ex) {
				return false;
			}
		}
		return true;
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
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		indexWriterConfig.setMaxBufferedDocs(500);
		indexWriterConfig.setRAMBufferSizeMB(300);		
		if(!isDirectoryOpen()) {
			directory = FSDirectory.open(new File(indexPath));
		}
		if (IndexWriter.isLocked(directory)) {
			IndexWriter.unlock(directory);
			log.info("Removed Lucene lock file in " + directory);
		}
		IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
		return writer;
	}
	
	public synchronized void closeDirectory() throws IOException{
		 //the file-based-directory is closed after every operation in order to avoid the "too many open files" exception in Linux
		 if (directory instanceof FSDirectory) {
			 IOUtils.close(directory);
		 }
	 }


}
