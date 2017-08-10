package net.whydah.identity.application.search;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class LuceneApplicationIndexer {
    public static final String FIELD_APPLICATIONID = "applicationid";
    public static final String FIELD_FULLJSON = "fulljson";
    public static final String FIELD_FULLSEARCH = "fullsearch";

    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_4;
    protected static final Analyzer ANALYZER = new StandardAnalyzer();

    private static final Logger log = LoggerFactory.getLogger(LuceneApplicationIndexer.class);
    private final Directory index;

    @Autowired
    public LuceneApplicationIndexer(Directory luceneApplicationDirectory) {
        //this.index = luceneApplicationDirectory;
        this.index = luceneApplicationDirectory;

        verifyWriter(index);
    }

    private void verifyWriter(Directory index) {
        //Open a writer to ensure segments* file is created.
        IndexWriter w = null;
        try {
            w = getWriter();
            log.trace("LuceneApplicationIndexer initialized. lockId={} ", index.getLockID());
        } catch (IOException e) {
            log.error("getWriter failed.", e);
        } finally {
            closeWriter(w);
        }
    }

    public void addToIndex(Application application) {
        IndexWriter w = null;
        try {
            w = getWriter();
            Document doc = createLuceneDocument(application);
            w.addDocument(doc);
        } catch (IOException e) {
            log.error("addToIndex failed for {}.", application.toString(), e);
        } finally {
            closeWriter(w);
        }
    }


    public void addToIndex(List<Application> applications) throws IOException {
        IndexWriter w = null;
        try {
            w = getWriter();
            for (Application application : applications) {
                try {
                    Document doc = createLuceneDocument(application);
                    w.addDocument(doc);
                } catch (IOException e) {
                    log.error("addToIndex failed for {}. Application was not added to lucene index.", applications.toString(), e);
                }
            }
        } finally {
            closeWriter(w);
        }
    }

    public void update(Application application) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.updateDocument(new Term(FIELD_APPLICATIONID, application.getId()), createLuceneDocument(application));
        } catch (IOException e) {
            log.error("updating {} failed.", application.toString(), e);
        } finally {
            closeWriter(w);
        }
    }

    public void removeFromIndex(String appId) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.deleteDocuments(new Term(FIELD_APPLICATIONID, appId));
        } catch (IOException e) {
            log.error("removeFromIndex failed. uid={}", appId, e);
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
        return new IndexWriter(index, new IndexWriterConfig(LUCENE_VERSION, ANALYZER));
    }

    private Document createLuceneDocument(Application application) {
        FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
        ftNotTokenized.setTokenized(false);
        ftNotTokenized.setIndexed(false);

        FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
        ftTokenized.setTokenized(true);

        FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);
        ftNotIndexed.setIndexed(false);


        Document doc = new Document();
        doc.add(new Field(FIELD_APPLICATIONID, application.getId(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
        doc.add(new Field(FIELD_FULLJSON, ApplicationMapper.toJson(application), ftNotTokenized));
        doc.add(new Field(FIELD_FULLSEARCH, ApplicationMapper.toPrettyJson(application), ftNotTokenized));
        return doc;
    }


    private void closeWriter(IndexWriter w) {
        if (w != null) {
            try {
                //w.optimize();
                w.close();
            } catch (IOException e) {
                //intentionally ignore
            }
        }
    }
}
