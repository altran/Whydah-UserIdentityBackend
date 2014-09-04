package net.whydah.identity.user.search;

import junit.framework.TestCase;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LuceneSearchTest extends TestCase{

  @Test
  public void testsearch() throws IOException {
        Directory index = new RAMDirectory();

        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
            
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        result = luceneSearch.search("Vuppala");
        assertEquals(1, result.size());
        result = luceneSearch.search("sunil@freecode.no");
        assertEquals(1, result.size());
        result = luceneSearch.search("Pølser");
        assertEquals(0, result.size());
        
    }
  
 
    @Test
    public void testremoveuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
            
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        luceneIndexer.removeFromIndex("sunil@freecode.no");
        result = luceneSearch.search("Sunil");
        assertEquals(0, result.size());
    }
    

    @Test
    public void testmodifyuser() throws IOException {
        RAMDirectory index = new RAMDirectory();
        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        luceneIndexer.update(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
       
    }

    @Test
    public void testwildcardsearch() throws IOException {
        Directory index = new RAMDirectory();
        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
          	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
        
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        result = luceneSearch.search("Sunil");
        assertEquals(1, result.size());
        result = luceneSearch.search("sunil@");
        assertEquals(1, result.size());
    }

    @Test
    public void testweights() throws IOException {
        Directory index = new RAMDirectory();

        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));

        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Vuppala");
        assertEquals(1, result.size());
        assertEquals("sunil@freecode.no", result.get(0).getUsername());
    }


    private static UserIdentity createUser(String username, String fornavn, String etternavn, String email, String uid) {
        UserIdentity user1 = new UserIdentity();
        user1.setUsername(username);
        user1.setFirstName(fornavn);
        user1.setLastName(etternavn);
        user1.setEmail(email);
        user1.setUid(uid);
	    user1.setPersonRef("r"+uid);
        return user1;
    }

}
