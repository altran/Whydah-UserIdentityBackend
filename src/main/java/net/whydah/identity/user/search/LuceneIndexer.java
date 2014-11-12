package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.UserIdentity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Indexer for adding users to the index.
 */
public class LuceneIndexer {
    public static final String FIELD_FIRSTNAME = "firstname";
    public static final String FIELD_LASTNAME = "surname";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PERSONREF = "personref";
    public static final String FIELD_MOBILE = "mobile";

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexer.class);
    private static final Analyzer ANALYZER = new StandardAnalyzer(Version.LUCENE_31);
    private final Directory index;

    public LuceneIndexer(Directory index) {
        this.index = index;

        //Open a writer to ensure segments* file is created.
        IndexWriter w = null;
        try {
            w = getWriter();
            LOGGER.debug("LuceneIndexer initialized. lockId={}", index.getLockID());
        } catch (IOException e) {
            LOGGER.error("getWriter failed.", e);
        } finally {
            closeWriter(w);
        }
    }

    public void addToIndex(UserIdentity user) {
        IndexWriter w = null;
        try {
            w = getWriter();
            Document doc = createLuceneDocument(user);
            w.addDocument(doc);
        } catch (IOException e) {
            LOGGER.error("addToIndex failed for {}.", user.toString(), e);
        } finally {
            closeWriter(w);
        }
    }


    public void addToIndex(List<UserIdentity> users) throws IOException {
        IndexWriter w = null;
        try {
            w = getWriter();
            for (UserIdentity user : users) {
                try {
                    Document doc = createLuceneDocument(user);
                    w.addDocument(doc);
                } catch (IOException e) {
                    LOGGER.error("addToIndex failed for {}. User was not added to lucene index.", user.toString(), e);
                }
            }
        } finally {
            closeWriter(w);
        }
    }

    public void update(UserIdentity user) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.updateDocument(new Term(FIELD_UID, user.getUid()), createLuceneDocument(user));
        } catch (IOException e) {
            LOGGER.error("updating {} failed.", user.toString(), e);
        } finally {
            closeWriter(w);
        }
    }

    public void removeFromIndex(String uid) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.deleteDocuments(new Term(FIELD_UID, uid));
        } catch (IOException e) {
            LOGGER.error("removeFromIndex failed. uid={}", uid, e);
        } finally {
            closeWriter(w);
        }
    }

    /**
     * Use with caution. Close writer after use.
     * @return IndexWriter
     * @throws org.apache.lucene.index.CorruptIndexException if the index is corrupt
     * @throws org.apache.lucene.store.LockObtainFailedException if another writer
     *  has this index open (<code>write.lock</code> could not
     *  be obtained)
     * @throws IOException if the directory cannot be
     *  read/written to or if there is any other low-level
     *  IO error
     */
    private IndexWriter getWriter() throws IOException {
        return new IndexWriter(index, ANALYZER, IndexWriter.MaxFieldLength.UNLIMITED);
    }


    private Document createLuceneDocument(UserIdentity user) {
        Document doc = new Document();
        doc.add(new Field(FIELD_FIRSTNAME, user.getFirstName(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(FIELD_LASTNAME, user.getLastName(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(FIELD_UID, user.getUid(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_USERNAME, user.getUsername(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(FIELD_EMAIL, user.getEmail(), Field.Store.YES, Field.Index.ANALYZED));
        if (user.getPersonRef() != null) {
            doc.add(new Field(FIELD_PERSONREF, user.getPersonRef(), Field.Store.YES, Field.Index.NO));
        }

        if (user.getCellPhone() != null) {
            doc.add(new Field(FIELD_MOBILE, user.getCellPhone(), Field.Store.YES, Field.Index.ANALYZED));
        }
        return doc;
    }

    private void closeWriter(IndexWriter w) {
        if (w != null) {
            try {
                w.optimize();
                w.close();
            } catch (IOException e) {
                //intentionally ignore
            }
        }
    }
}
