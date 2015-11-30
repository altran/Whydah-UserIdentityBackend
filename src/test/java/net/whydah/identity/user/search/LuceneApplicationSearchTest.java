package net.whydah.identity.user.search;

import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.application.search.LuceneApplicationSearch;
import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LuceneApplicationSearchTest {




    @Test
    public void testApplicationSearch() throws IOException {
        RAMDirectory index = new RAMDirectory();
        addApplications(index);

        LuceneApplicationSearch luceneSearch = new LuceneApplicationSearch(index);
        List<Application> result = luceneSearch.search("Whydah");
        assertEquals(9, result.size());
        List<Application> result2 = luceneSearch.search("SecurityTokenService");
        assertEquals(1, result2.size());
        List<Application> result3 = luceneSearch.search("ACS");
        assertEquals(2, result3.size());
        List<Application> result4 = luceneSearch.search("whydahdev");
        assertEquals(6, result4.size());
        List<Application> result5 = luceneSearch.search("*");
        assertEquals(9, result5.size());

    }




    private LuceneApplicationIndexer addApplications(Directory index) throws IOException {
        LuceneApplicationIndexer luceneIndexer = new LuceneApplicationIndexer(index);
        List<Application> applications = ApplicationMapper.fromJsonList(ApplicationHelper.getDummyAppllicationListJson());
        luceneIndexer.addToIndex(applications);
        return luceneIndexer;
    }

}
