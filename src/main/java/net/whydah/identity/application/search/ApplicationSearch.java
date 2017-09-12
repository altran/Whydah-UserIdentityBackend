package net.whydah.identity.application.search;


import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationSearch {

        private static final Logger log = LoggerFactory.getLogger(ApplicationSearch.class);
    private final LuceneApplicationSearch luceneApplicationSearch;
    private final LuceneApplicationIndexer luceneApplicationIndexer;

        @Autowired
        public ApplicationSearch(LuceneApplicationSearch luceneApplicationSearch, LuceneApplicationIndexer luceneApplicationIndexer) {
            this.luceneApplicationSearch = luceneApplicationSearch;
            this.luceneApplicationIndexer = luceneApplicationIndexer;
        }

        public List<Application> search(String query) {
            List<Application> applications = luceneApplicationSearch.search(query);
            if (applications == null) {
                applications = new ArrayList<>();
            }
            log.warn("lucene application search with query={} returned {} applications.", query, applications.size());

            return applications;
        }

    public int getApplicationIndexSize() {
        return luceneApplicationSearch.getApplicationIndexSize();
    }
    }
