package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LuceneSearch {
    private static final Logger logger = LoggerFactory.getLogger(LuceneSearch.class);
    private static final Analyzer ANALYZER = new StandardAnalyzer(Version.LUCENE_31);
    private static final int MAX_HITS = 200;
    private final Directory index;


    public LuceneSearch(Directory index) {
        this.index = index;
    }

    public List<UserIdentityRepresentation> search(String queryString) {
        String wildCardQuery = buildWildCardQuery(queryString);
        String[] fields = {
                LuceneIndexer.FIELD_FIRSTNAME,
                LuceneIndexer.FIELD_LASTNAME,
                LuceneIndexer.FIELD_EMAIL,
                LuceneIndexer.FIELD_USERNAME,
                LuceneIndexer.FIELD_MOBILE
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(LuceneIndexer.FIELD_FIRSTNAME, 2.5f);
        boosts.put(LuceneIndexer.FIELD_LASTNAME, 2f);
        boosts.put(LuceneIndexer.FIELD_USERNAME, 1.5f);
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_30, fields, ANALYZER, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);
        Query q;
        try {
            q = multiFieldQueryParser.parse(wildCardQuery);
        } catch (ParseException e) {
            logger.error("Could not parse wildCardQuery={}. Returning empty search result.", wildCardQuery, e);
            return new ArrayList<>();
        }

        List<UserIdentityRepresentation> result = new ArrayList<>();
        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher(index, true);
            TopDocs topDocs = searcher.search(q, MAX_HITS);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                UserIdentity user = new UserIdentity();
                user.setFirstName(d.get(LuceneIndexer.FIELD_FIRSTNAME));
                user.setLastName(d.get(LuceneIndexer.FIELD_LASTNAME));
                user.setUid(d.get(LuceneIndexer.FIELD_UID));
                user.setUsername(d.get(LuceneIndexer.FIELD_USERNAME));
                user.setPersonRef(d.get(LuceneIndexer.FIELD_PERSONREF));
                user.setCellPhone(d.get(LuceneIndexer.FIELD_MOBILE));
                user.setEmail(d.get(LuceneIndexer.FIELD_EMAIL));
                //System.out.println(user.getUsername() + " : " + hit.score);
                result.add(user);
            }
        } catch (IOException e) {
            logger.error("Error when searching.", e);
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException e) {
                    logger.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return result;
    }

    private String buildWildCardQuery(String queryString) {
        String[] queryitems = queryString.replace('_', ' ').split(" ");
        StringBuilder strb = new StringBuilder();
        for (String queryitem : queryitems) {
            strb.append(queryitem).append("^2 ");
            strb.append(queryitem).append("* ");
        }
        String wildCardQuery = strb.toString();
        logger.debug("Original query={}, wildcard query= {}", queryString, wildCardQuery);
        return wildCardQuery;
    }
}
