package net.whydah.identity.user.search;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import net.whydah.identity.application.search.LuceneApplicationIndexerTest.DirectoryType;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.sso.user.types.UserIdentity;

public class LuceneUserIndexerTest {

	LuceneUserIndexerImpl indexer;
	DirectoryType type = DirectoryType.NIOF; //we can switch to RAM
	Directory dir = null;

	public enum DirectoryType {
		RAM,
		NIOF
	}

	@Before
	public void beforeTest() throws Exception {		
		System.out.println("initializing before tests...");
		initDirectory();
	}

	private void initDirectory() throws IOException {
		dir = null;
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			if (!path.exists()) {
				path.mkdir();
			} else {
				FileSystemUtils.deleteRecursively(path);
				path.mkdir();
			}
			dir = new NIOFSDirectory(path);
		} else if(type ==  DirectoryType.RAM){
			dir = new RAMDirectory();
		}

		if(dir!=null) {
			indexer = new LuceneUserIndexerImpl(dir);
			System.out.println("initialized");
		} else {
			fail("Initialization failed. No directory found");
		}
	}
	
	@After
	public void afterTest() throws Exception {		
		System.out.println("tear down after tests...");
		
		File path = new File("lunceneUserIndexDirectoryTest");
		FileSystemUtils.deleteRecursively(path);
	}

	private static LDAPUserIdentity getRandomUser() {
		String uuid = UUID.randomUUID().toString();
		LDAPUserIdentity user1 = new LDAPUserIdentity();
		user1.setUsername(uuid);
		user1.setFirstName(randomIdentifier());
		user1.setLastName(randomIdentifier());
		user1.setEmail(uuid + "_email@gmail.com");
		user1.setUid(uuid);
		user1.setPersonRef("r_" + uuid);
		return user1;
	}
	

	public static String randomIdentifier() {
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final java.util.Random rand = new java.util.Random();
	    StringBuilder builder = new StringBuilder();
	    while(builder.toString().length() == 0) {
	        int length = rand.nextInt(5)+5;
	        for(int i = 0; i < length; i++) {
	            builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
	        }
	    }
	    return builder.toString();
	}

	//testing adding with multithreading
	@Test
	public void testAddingASingleUser() throws Exception {
		List<Thread> ts = new ArrayList<>();
		
		for(int i=0; i<100; i++) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					UserIdentity i = getRandomUser();
					boolean ok = indexer.addToIndex(i); //should always return true under normal condition (i.e. enough memory/disk space)
					System.out.println(Thread.currentThread().getName() + ( ok? " added item to index":" added item to queue"));					
				}
			});
			
			t.setName("thread_" + i);
			t.start();
			ts.add(t);
		}

		//wait for all threads to complete before we check the result
		for(Thread t : ts) {
			t.join();
		}
		
		
		Thread.sleep(2000);
		System.out.println("Thread count " + ts.size());
		System.out.println("Total records: " + indexer.numDocs());
		assertTrue(indexer.numDocs()==ts.size());

	}
	
	@Test
	public void testUpdatingASingleUser() throws Exception {		
		UserIdentity i = getRandomUser();
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		i.setFirstName("Shakespeare");
		i.setLastName("William");
		boolean updateResult = indexer.updateIndex(i);		
		assertTrue(updateResult);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			dir = new NIOFSDirectory(path);
		}
		
		LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(dir);
		List<UserIdentity> result = luceneSearch.search("William Shakespeare");
		assertTrue(result.size()==1);
	}
	
	@Test
	public void testRemovingASingleUser() {
		UserIdentity i = getRandomUser();
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		boolean removeResult = indexer.removeFromIndex(i.getUid());	
		assertTrue(removeResult);
		assertTrue(indexer.numDocs()==0);
	}
	
	@Test
	public void testAllOperationsWithMultiThreads() throws Exception {
		List<Thread> ts = new ArrayList<>();
		AtomicInteger numberOfItemsRemoved = new AtomicInteger(0);
		AtomicInteger numberOfItemsUpdated = new AtomicInteger(0);
		
		for(int i=0; i<100; i++) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					UserIdentity i = getRandomUser();
					boolean addResult = indexer.addToIndex(i);
					System.out.println(Thread.currentThread().getName() + " added its user to the index");
					assertTrue(addResult);//should return true under normal condition (i.e. enough memory/disk space)
					if(new Random().nextBoolean()) {
						i.setFirstName("Shakespeare");
						i.setLastName("William");
						boolean updateResult = indexer.updateIndex(i);
						assertTrue(updateResult);
						System.out.println(Thread.currentThread().getName() + " updated its user");	
						numberOfItemsUpdated.getAndIncrement();
					} else {
						boolean removeResult = indexer.removeFromIndex(i.getUid());
						assertTrue(removeResult);
						System.out.println(Thread.currentThread().getName() + " removed its user out of the index list");	
						numberOfItemsRemoved.getAndIncrement();
					}
					
					
				}
			});
			
			t.setName("thread_" + i);
			t.start();
			ts.add(t);
		}
		
		
		
		//wait for all threads to complete before we check the result
		for(Thread t : ts) {
			t.join();
		}
		
	
		
		Thread.sleep(5000);
		System.out.println("Thread count " + ts.size());	
		System.out.println("Total items updated: " + numberOfItemsUpdated.get());
		System.out.println("Total items removed: " + numberOfItemsRemoved.get());
		System.out.println("Total records (exlude deletion): " + indexer.numDocs());
		assertTrue(indexer.numDocs()==ts.size() - numberOfItemsRemoved.get());

	}

	@Test
	public void testAddingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		for(int i=0; i<1000; i++) {	
			list.add(getRandomUser());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==1000);
	}

	@Test
	public void testUpdatingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		for(int i=0; i<100; i++) {	
			list.add(getRandomUser());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==100);
		
		for(int i=0; i<100; i++) {	
			list.get(i).setFirstName("Henry");
		}
		indexer.updateIndex(list);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			dir = new NIOFSDirectory(path);
		}

		LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(dir);
		List<UserIdentity> result = luceneSearch.search("Henry");
		assertTrue(result.size()==100);
	}
	
	@Test
	public void testDeletingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		List<String> uuidList = new ArrayList<String>();
		for(int i=0; i<100; i++) {	
			UserIdentity u = getRandomUser();
			list.add(u);
			uuidList.add(u.getUid());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==100);
		
		indexer.removeFromIndex(uuidList);
		assertTrue(indexer.numDocs()==0);
	}
	
	

}
