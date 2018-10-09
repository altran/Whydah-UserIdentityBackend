package net.whydah.identity.application.search;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LuceneApplicationIndexer extends LuceneApplicationIndexerImpl{
    
    @Autowired
    public LuceneApplicationIndexer(@Qualifier("luceneApplicationDirectory") Directory luceneApplicationDirectory) throws IOException {
        super(luceneApplicationDirectory);
    }

}
