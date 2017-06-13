package net.whydah.identity.user.search;

import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.identity.UIBUserIdentity;
import net.whydah.sso.user.mappers.UserAggregateMapper;
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
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Indexer for adding users to the index.
 */
@Service
public class LuceneUserIndexer {
    public static final String FIELD_FIRSTNAME = "firstname";
    public static final String FIELD_LASTNAME = "surname";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PERSONREF = "personref";
    public static final String FIELD_MOBILE = "mobile";
    public static final String FIELD_FULLJSON = "fulljson";

    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_4;
    protected static final Analyzer ANALYZER = new StandardAnalyzer();

    private static final Logger log = LoggerFactory.getLogger(LuceneUserIndexer.class);
    private static Directory index = null;

    private static FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
    private static FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
    private static FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);

    private static IndexWriter indexWriter;

    /*
    @Autowired
    @Configure
    public LuceneIndexer(@Configuration("lucene.directory") String luceneDir) {
        this.index = createDirectory(luceneDir);
        verifyWriter(index);
    }
    */

    /*
    private NIOFSDirectory createDirectory(String luceneDir) {
        try {
            File luceneDirectory = new File(luceneDir);
            if (!luceneDirectory.exists()) {
                boolean dirsCreated = luceneDirectory.mkdirs();
                if (!dirsCreated) {
                    log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
                }
            }
            return new NIOFSDirectory(luceneDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    */

    @Autowired
    public LuceneUserIndexer(Directory myindex) {
        indexWriter = null;
        ftNotTokenized.setTokenized(false);
        ftTokenized.setTokenized(true);
        ftNotIndexed.setIndexed(false);

        index = myindex;
        for (int n = 0; n < 3; n++) {
            verifyWriter();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }


        }
    }

    private void verifyWriter() {
        try {
            indexWriter = getWriter();

            log.trace("LuceneUserIndexer initialized. lockId={}", index.getLockID());
        } catch (IOException e) {
            log.error("getWriter failed.", e);

        } finally {
            closeIndexer();
        }
    }

    public void addToIndex(UIBUserIdentity user) {
        try {
            getWriter();
            Document doc = createLuceneDocument(user);
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            log.error("addToIndex failed for {}.", user.toString(), e);
        } finally {
            closeWriter();
        }
    }

    public void addToUserAggregateIndex(UIBUserAggregate userAggregate) {
        try {
            getWriter();
            Document doc = createLuceneDocument(userAggregate);
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            log.error("addToUserAggregateIndex failed for {}.", userAggregate.toString(), e);
        } finally {
            closeWriter();
        }
    }


    public void addToIndex(List<UIBUserIdentity> users) throws IOException {
        try {
            getWriter();
            for (UIBUserIdentity user : users) {
                try {
                    Document doc = createLuceneDocument(user);
                    indexWriter.addDocument(doc);
                } catch (IOException e) {
                    log.error("addToIndex failed for {}. User was not added to lucene index.", user.toString(), e);
                }
            }
        } finally {
            closeWriter();
            //indexWriter=null;
        }
    }


    public void addToUserAggregateIndex(List<UIBUserAggregate> userAggregates) throws IOException {
        try {
            getWriter();
            for (UIBUserAggregate userAggregate : userAggregates) {
                try {
                    Document doc = createLuceneDocument(userAggregate);
                    indexWriter.addDocument(doc);
                } catch (IOException e) {
                    log.error("addToUserAggregateIndex failed for {}. User was not added to lucene index.", userAggregate.toString(), e);
                }
            }
        } finally {
            closeWriter();
        }
    }


    public void update(UIBUserIdentity user) {
        try {
            getWriter();
            indexWriter.updateDocument(new Term(FIELD_UID, user.getUid()), createLuceneDocument(user));
        } catch (IOException e) {
            log.error("updating {} failed.", user.toString(), e);
        } finally {
            closeWriter();
        }
    }

    public void updateUserAggregate(UIBUserAggregate userAggregate) {
        try {
            getWriter();
            indexWriter.updateDocument(new Term(FIELD_UID, userAggregate.getUid()), createLuceneDocument(userAggregate));
        } catch (IOException e) {
            log.error("updating {} failed.", userAggregate.toString(), e);
        } finally {
            closeWriter();
        }
    }

    public void removeFromIndex(String uid) {
        try {
            getWriter();
            indexWriter.deleteDocuments(new Term(FIELD_UID, uid));
        } catch (IOException e) {
            log.error("removeFromIndex failed. uid={}", uid, e);
        } finally {
            closeWriter();
        }
    }

    /**
     * Use with caution. Close writer after use.
     *
     * @return IndexWriter
     * @throws org.apache.lucene.index.CorruptIndexException     if the index is corrupt
     * @throws org.apache.lucene.store.LockObtainFailedException if another writer
     *                                                           has this index open (<code>write.lock</code> could not
     *                                                           be obtained)
     * @throws IOException                                       if the directory cannot be
     *                                                           read/written to or if there is any other low-level
     *                                                           IO error
     */
    private IndexWriter getWriter() throws IOException {
        if (index instanceof RAMDirectory) {
            indexWriter = new IndexWriter(index, new IndexWriterConfig(LUCENE_VERSION, ANALYZER));
            return indexWriter;
        }
        if (indexWriter != null) {
            return indexWriter;
        }
        try {
            File directory = new File(index.toString());
            if (!directory.exists()) {
                directory.mkdir();
                // If you require it to make the entire directory path including parents,
                // use directory.mkdirs(); here instead.
            }
            closeIndexer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
            indexWriterConfig.setMaxBufferedDocs(500);
            indexWriterConfig.setRAMBufferSizeMB(300);
            indexWriter = new IndexWriter(index, indexWriterConfig);

            return indexWriter;
        } catch (LockObtainFailedException e) {
            log.warn("Unable to access lock to lucene index worker", e);
            closeIndexer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
            indexWriterConfig.setMaxBufferedDocs(500);
            indexWriterConfig.setRAMBufferSizeMB(300);
            indexWriterConfig.setWriteLockTimeout(10000);
            indexWriter = new IndexWriter(index, indexWriterConfig);
            if (indexWriter != null) {
                return indexWriter;

            }
        }

//        getWriter();  // lets try once more...
        throw new IOException("Unable to access lock to lucene index worker ");
    }

    private Document createLuceneDocument(UIBUserIdentity user) {


        Document doc = new Document();
        doc.add(new Field(FIELD_UID, user.getUid(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
        doc.add(new Field(FIELD_USERNAME, user.getUsername(), ftTokenized));
        doc.add(new Field(FIELD_EMAIL, user.getEmail(), ftTokenized));

        if (user.getFirstName() != null) {
            doc.add(new Field(FIELD_FIRSTNAME, user.getFirstName(), ftTokenized));
        }
        if (user.getLastName() != null) {
            doc.add(new Field(FIELD_LASTNAME, user.getLastName(), ftTokenized));
        }
        if (user.getPersonRef() != null) {
            doc.add(new Field(FIELD_PERSONREF, user.getPersonRef(), ftNotIndexed));  //Field.Index.NO
        }

        if (user.getCellPhone() != null) {
            doc.add(new Field(FIELD_MOBILE, user.getCellPhone(), ftTokenized));
        }
        return doc;
    }

    private Document createLuceneDocument(UIBUserAggregate userAggregate) {

        Document doc = new Document();
        doc.add(new Field(FIELD_UID, userAggregate.getUid(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
        doc.add(new Field(FIELD_USERNAME, userAggregate.getUsername(), ftTokenized));
        doc.add(new Field(FIELD_EMAIL, userAggregate.getEmail(), ftTokenized));

        if (userAggregate.getFirstName() != null) {
            doc.add(new Field(FIELD_FIRSTNAME, userAggregate.getFirstName(), ftTokenized));
        }
        if (userAggregate.getLastName() != null) {
            doc.add(new Field(FIELD_LASTNAME, userAggregate.getLastName(), ftTokenized));
        }
        if (userAggregate.getPersonRef() != null) {
            doc.add(new Field(FIELD_PERSONREF, userAggregate.getPersonRef(), ftNotIndexed));  //Field.Index.NO
        }

        if (userAggregate.getCellPhone() != null) {
            doc.add(new Field(FIELD_MOBILE, userAggregate.getCellPhone(), ftTokenized));
        }
        doc.add(new Field(FIELD_FULLJSON, UserAggregateMapper.toJson(userAggregate), ftTokenized));
        return doc;
    }


    private void closeWriter() {
        if (indexWriter != null) {
            try {
                //w.optimize();
                if (index instanceof RAMDirectory) {
                    indexWriter.close();
                    indexWriter = null;
                } else {
                    indexWriter.close();
                    indexWriter = null;
                }
            } catch (IOException e) {
                //intentionally ignore
            } finally {

            }
        }
    }

    public static void closeIndexer() {

        if (indexWriter == null) {
            return;
        }
        try {
            indexWriter.commit();
            indexWriter.close();
//            Thread.sleep(2000);
            indexWriter = null;
//            Path fileToDeletePath = Paths.get(index.toString()+File.separator+"write.lock");
//            Files.delete(fileToDeletePath);
        } catch (Exception e) {
            log.warn("Exception in closeIndexer", e);
            //intentionally ignore
        } finally {
            //indexWriter = null;


        }

    }
}
