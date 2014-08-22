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

public class Search {
    private static final Logger logger = LoggerFactory.getLogger(Search.class);
    private static final Analyzer ANALYZER = new StandardAnalyzer(Version.LUCENE_31);
    private static final int MAX_HITS = 200;
    private final Directory index;


    public Search(Directory index) {
        this.index = index;
    }

    public List<UserIdentityRepresentation> search(String queryString) {
        String wildCardQuery = buildWildCardQuery(queryString);
        String[] fields = {
                Indexer.FIELD_FIRSTNAME,
                Indexer.FIELD_LASTNAME,
                Indexer.FIELD_EMAIL,
                Indexer.FIELD_USERNAME,
                Indexer.FIELD_MOBILE
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(Indexer.FIELD_FIRSTNAME, 2.5f);
        boosts.put(Indexer.FIELD_LASTNAME, 2f);
        boosts.put(Indexer.FIELD_USERNAME, 1.5f);
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
                user.setFirstName(d.get(Indexer.FIELD_FIRSTNAME));
                user.setLastName(d.get(Indexer.FIELD_LASTNAME));
                user.setUid(d.get(Indexer.FIELD_UID));
                user.setUsername(d.get(Indexer.FIELD_USERNAME));
                user.setPersonRef(d.get(Indexer.FIELD_PERSONREF));
                user.setCellPhone(d.get(Indexer.FIELD_MOBILE));
                user.setEmail(d.get(Indexer.FIELD_EMAIL));
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
