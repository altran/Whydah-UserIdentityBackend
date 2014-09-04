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
        	add(createUser("kari.norman@example.com", "Kari", "Norman", "kari.norman@example.com", "kari.norman@example.com"));
        	add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        	add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));
            
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Norman");
        assertEquals(2, result.size());
        result = luceneSearch.search("ola@example.com");
        assertEquals(1, result.size());
        result = luceneSearch.search("Pølser");
        assertEquals(0, result.size());
        
    }
  
 
    @Test
    public void testremoveuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("kari.norman@example.com", "Kari", "norman", "kari.norman@example.com", "kari.norman@example.com"));
        	add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        	add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));
            
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        luceneIndexer.removeFromIndex("ola@example.com");
        result = luceneSearch.search("Ola");
        assertEquals(0, result.size());
    }
    

    @Test
    public void testmodifyuser() throws IOException {
        RAMDirectory index = new RAMDirectory();
        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("kari.norman@example.com", "Kari", "norman", "kari.norman@example.com", "kari.norman@example.com"));
        	add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        	add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        luceneIndexer.update(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
       
    }

    @Test
    public void testwildcardsearch() throws IOException {
        Directory index = new RAMDirectory();
        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
          	add(createUser("kari.norman@example.com", "Kari", "norman", "kari.norman@example.com", "kari.norman@example.com"));
        	add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        	add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));
        
        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("ola@");
        assertEquals(1, result.size());
    }

    @Test
    public void testweights() throws IOException {
        Directory index = new RAMDirectory();

        LuceneIndexer luceneIndexer = new LuceneIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>(){{
        	add(createUser("kari.norman@example.com", "Kari", "Norman", "kari.norman@example.com", "kari.norman@example.com"));
        	add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        	add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));

        }};
        luceneIndexer.addToIndex(users);
        LuceneSearch luceneSearch = new LuceneSearch(index);
        List<UserIdentityRepresentation> result = luceneSearch.search("Norman");
        assertEquals(2, result.size());
        assertEquals("ola@example.com", result.get(1).getUsername());
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
