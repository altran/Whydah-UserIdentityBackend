package net.whydah.identity.search;

import junit.framework.TestCase;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchTest extends TestCase{

  @Test
  public void testsearch() throws IOException {
        Directory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<WhydahUserIdentity> users = new ArrayList<WhydahUserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
            
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<WhydahUserIdentity> result = search.search("Sunil");
        assertEquals(1, result.size());
        result = search.search("Vuppala");
        assertEquals(1, result.size());
        result = search.search("sunil@freecode.no");
        assertEquals(1, result.size());
        result = search.search("Pølser");
        assertEquals(0, result.size());
        
    }
  
 
    @Test
    public void testremoveuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<WhydahUserIdentity> users = new ArrayList<WhydahUserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
            
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<WhydahUserIdentity> result = search.search("Sunil");
        assertEquals(1, result.size());
        indexer.removeFromIndex("sunil@freecode.no");
        result = search.search("Sunil");
        assertEquals(0, result.size());
    }
    

    @Test
    public void testmodifyuser() throws IOException {
        RAMDirectory index = new RAMDirectory();
        Indexer indexer = new Indexer(index);
        List<WhydahUserIdentity> users = new ArrayList<WhydahUserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<WhydahUserIdentity> result = search.search("Sunil");
        assertEquals(1, result.size());
        result = search.search("Sunil");
        assertEquals(1, result.size());
        indexer.update(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        result = search.search("Sunil");
        assertEquals(1, result.size());
        result = search.search("Sunil");
        assertEquals(1, result.size());
       
    }

    @Test
    public void testwildcardsearch() throws IOException {
        Directory index = new RAMDirectory();
        Indexer indexer = new Indexer(index);
        List<WhydahUserIdentity> users = new ArrayList<WhydahUserIdentity>(){{
          	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));
        
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<WhydahUserIdentity> result = search.search("Sunil");
        assertEquals(1, result.size());
        result = search.search("Sunil");
        assertEquals(1, result.size());
        result = search.search("sunil@");
        assertEquals(1, result.size());
    }

    @Test
    public void testweights() throws IOException {
        Directory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<WhydahUserIdentity> users = new ArrayList<WhydahUserIdentity>(){{
        	add(createUser("rafal.laczek@freecode.no", "Rafal", "Laczek", "rafal.laczek@freecode.no", "rafal.laczek@freecode.no"));
        	add(createUser("sunil@freecode.no", "Sunil", "Vuppala", "sunil@freecode.no", "sunil@freecode.no"));
        	add(createUser("frode.torvund@freecode.no", "Frode", "Torvund", "frode.torvund@freecode.no", "frode.torvund@freecode.no"));

        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<WhydahUserIdentity> result = search.search("Vuppala");
        assertEquals(1, result.size());
        assertEquals("sunil@freecode.no", result.get(0).getUsername());
    }


    private static WhydahUserIdentity createUser(String username, String fornavn, String etternavn, String email, String uid) {
        WhydahUserIdentity user1 = new WhydahUserIdentity();
        user1.setUsername(username);
        user1.setFirstName(fornavn);
        user1.setLastName(etternavn);
        user1.setEmail(email);
        user1.setUid(uid);
	    user1.setPersonRef("r"+uid);
        return user1;
    }

}
