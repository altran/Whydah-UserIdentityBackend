package net.whydah.identity.search;

import net.whydah.identity.domain.WhydahUserIdentity;
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
    private final Directory index;

    public Search(Directory index) {
        this.index = index;
    }

    public List<WhydahUserIdentity> search(String queryString) {
        String[] queryitems = queryString.replace('_', ' ').split(" ");
        StringBuilder wilcardquery = new StringBuilder();
        for (String queryitem : queryitems) {
            wilcardquery.append(queryitem).append("^2 ");
            wilcardquery.append(queryitem).append("* ");
        }
        logger.debug("query now: {}", wilcardquery);


        int maxHits = 20;
        ArrayList<WhydahUserIdentity> result = new ArrayList<WhydahUserIdentity>();
        try {
            String[] fields = {
                    Indexer.FIELD_FIRSTNAME,
                    Indexer.FIELD_LASTNAME,
                    Indexer.FIELD_EMAIL,
                    Indexer.FIELD_USERNAME,
                    Indexer.FIELD_MOBILE
            };
            HashMap<String, Float> boosts = new HashMap<String, Float>();
            boosts.put(Indexer.FIELD_FIRSTNAME, 2.5f);
            boosts.put(Indexer.FIELD_LASTNAME, 2f);
            boosts.put(Indexer.FIELD_USERNAME, 1.5f);
            MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_30, fields, ANALYZER, boosts);
            multiFieldQueryParser.setAllowLeadingWildcard(true);
            Query q = multiFieldQueryParser.parse(wilcardquery.toString());
            IndexSearcher searcher = new IndexSearcher(index, true);
            TopDocs topDocs = searcher.search(q, maxHits);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                WhydahUserIdentity user = new WhydahUserIdentity();
                user.setFirstName(d.get(Indexer.FIELD_FIRSTNAME));
                user.setLastName(d.get(Indexer.FIELD_LASTNAME));
                user.setUid(d.get(Indexer.FIELD_UID));
                user.setUsername(d.get(Indexer.FIELD_USERNAME));
                user.setPersonRef(d.get(Indexer.FIELD_PERSONREF));
                user.setCellPhone(d.get(Indexer.FIELD_MOBILE));
                user.setEmail(d.get(Indexer.FIELD_EMAIL));
//                System.out.println(user.getUsername() + " : " + hit.score);
                result.add(user);
            }
            searcher.close();
        } catch (ParseException e) {
            logger.error(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        return result;

    }

}
