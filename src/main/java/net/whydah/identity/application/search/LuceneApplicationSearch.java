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

@Service
public class LuceneApplicationSearch extends LuceneApplicationSearchImpl{
    private static final Logger log = LoggerFactory.getLogger(LuceneUserSearch.class);
    protected static final Analyzer ANALYZER = new StandardAnalyzer();  //use LuceneUserIndexer.ANALYZER?
    public static final int MAX_HITS = 500;
    
    @Autowired
    public LuceneApplicationSearch(@Qualifier("luceneApplicationDirectory") Directory luceneApplicationDirectory) {
       super(luceneApplicationDirectory);
    }


}
