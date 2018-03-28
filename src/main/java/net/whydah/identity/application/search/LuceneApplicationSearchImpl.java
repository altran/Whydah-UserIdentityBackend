package net.whydah.identity.application.search;


import net.whydah.identity.user.search.LuceneUserSearch;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LuceneApplicationSearchImpl {
    private static final Logger log = LoggerFactory.getLogger(LuceneUserSearch.class);
    protected static final Analyzer ANALYZER = new StandardAnalyzer();  //use LuceneUserIndexer.ANALYZER?
    public static final int MAX_HITS = 500;
    private final Directory index;


    public LuceneApplicationSearchImpl(Directory luceneApplicationDirectory) {
        this.index = luceneApplicationDirectory;
    }


    public List<Application> search(String queryString) {
        String wildCardQuery = buildWildCardQuery(queryString);
        log.trace("findApplications - Seach Application index - query:{}", wildCardQuery);
        String[] fields = {
                LuceneApplicationIndexer.FIELD_APPLICATIONID,
                LuceneApplicationIndexer.FIELD_FULLJSON,
                LuceneApplicationIndexer.FIELD_FULLSEARCH
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(LuceneApplicationIndexer.FIELD_APPLICATIONID, 2.5f);
        boosts.put(LuceneApplicationIndexer.FIELD_FULLJSON, 2f);
        boosts.put(LuceneApplicationIndexer.FIELD_FULLSEARCH, 1f);

        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, ANALYZER, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);

        Query q;
        try {
            q = multiFieldQueryParser.parse(wildCardQuery);

        } catch (ParseException e) {
            log.error("findApplications - ould not parse wildCardQuery={}. Returning empty search result.", wildCardQuery, e);
            return new ArrayList<>();
        }

        List<Application> result = new ArrayList<>();
        DirectoryReader directoryReader = null;
        try {
            //searcher = new IndexSearcher(index, true);    //http://lucene.472066.n3.nabble.com/IndexSearcher-close-removed-in-4-0-td4041177.html
            directoryReader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(q, MAX_HITS);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                Application application = ApplicationMapper.fromJson(d.get(LuceneApplicationIndexer.FIELD_FULLJSON));
                log.debug("findApplications- " + application.toString() + " : " + q + ":" + hit.score);
                result.add(application);
            }
        } catch (IOException e) {
            log.error("findApplications - Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                } catch (IOException e) {
                    log.info("findApplications - searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return result;
    }

    public List<Application> searchApplicationID(String queryString) {
        Term term = new Term(LuceneApplicationIndexer.FIELD_APPLICATIONID, queryString);
        TermQuery termQuery = new TermQuery(term);
        List<Application> result = new ArrayList<>();
        DirectoryReader directoryReader = null;
        try {
            //searcher = new IndexSearcher(index, true);    //http://lucene.472066.n3.nabble.com/IndexSearcher-close-removed-in-4-0-td4041177.html
            directoryReader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(termQuery, MAX_HITS);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                Application application = ApplicationMapper.fromJson(d.get(LuceneApplicationIndexer.FIELD_FULLJSON));
                log.trace(application.toString() + " : " + termQuery + ":" + hit.score);
                result.add(application);
            }
        } catch (IOException e) {
            log.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                } catch (IOException e) {
                    log.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return result;
    }

    public List<Application> searchApplicationName(String queryString) {
        Term term = new Term(LuceneApplicationIndexer.FIELD_APPLICATIONNAME, "*" + queryString + "*");
        Query wildcardQuery = new WildcardQuery(term);

        List<Application> result = new ArrayList<>();
        DirectoryReader directoryReader = null;
        try {
            //searcher = new IndexSearcher(index, true);    //http://lucene.472066.n3.nabble.com/IndexSearcher-close-removed-in-4-0-td4041177.html
            directoryReader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(wildcardQuery, MAX_HITS);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                Application application = ApplicationMapper.fromJson(d.get(LuceneApplicationIndexer.FIELD_FULLJSON));
                log.trace(application.toString() + " : " + wildcardQuery + ":" + hit.score);
                result.add(application);
            }
        } catch (IOException e) {
            log.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                } catch (IOException e) {
                    log.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return result;
    }
    private String buildWildCardQuery(String queryString) {
        queryString=queryString.replace("_","").trim();
        String[] queryitems = queryString.split(" ");
        StringBuilder strb = new StringBuilder();
        for (String queryitem : queryitems) {
            strb.append(queryitem).append("^2 ");
            strb.append(queryitem).append("* ");
        }
        String wildCardQuery = strb.toString();
        log.debug("Original query={}, wildcard query= {}", queryString, wildCardQuery);
        return wildCardQuery;
    }

    public int getApplicationIndexSize() {
        DirectoryReader directoryReader = null;
        try {
        	directoryReader = DirectoryReader.open(index);
            return directoryReader.numDocs();
        } catch (IOException e) {
            log.error("Error when accessing index.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                } catch (IOException e) {
                    log.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }
        return 0;

    }

}
