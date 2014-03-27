package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import net.whydah.identity.user.identity.LDAPHelper;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.search.Indexer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class WhydahUserIdentityImporter {

	private static final Logger logger = LoggerFactory.getLogger(WhydahUserIdentityImporter.class);

	private static final int REQUIRED_NUMBER_OF_FIELDS = 8;
	private static final int USERID = 0;
	private static final int USERNAME = 1;
	private static final int PASSWORD = 2;
	private static final int FIRSTNAME = 3;
	private static final int LASTNAME = 4;
	private static final int EMAIL = 5;
	private static final int CELLPHONE = 6;
	private static final int PERSONREF = 7;
	
    private LDAPHelper ldapHelper;
    private Directory index;
    
    @Inject
	public WhydahUserIdentityImporter(LDAPHelper ldapHelper, Directory index) {
		this.ldapHelper = ldapHelper;
		this.index = index;
	}
    
    public List<WhydahUserIdentity> importUsers(String userImportSource) {
    	List<WhydahUserIdentity> users = parseUsers(userImportSource);
    	
    	saveUsers(users);

    	return users;
    }

	private void saveUsers(List<WhydahUserIdentity> users) {
		try {
			Indexer indexer = new Indexer(index);
			final IndexWriter indexWriter = indexer.getWriter();
			for (WhydahUserIdentity userIdentity : users) {
				ldapHelper.addWhydahUserIdentity(userIdentity);
				indexer.addToIndex(indexWriter, userIdentity);
			}
	        indexWriter.optimize();
	        indexWriter.close();
		} catch (Exception e) {
			logger.error("Error importing from users!", e);
			throw new RuntimeException("Error importing users!", e);
		}
	}

	protected static List<WhydahUserIdentity> parseUsers(String userImportSource) {
		BufferedReader reader = null;
		try {
			List<WhydahUserIdentity> users = new ArrayList<>();
			logger.info("Importing data from {}", userImportSource);
			InputStream classpathStream = WhydahUserIdentityImporter.class.getClassLoader().getResourceAsStream(userImportSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	WhydahUserIdentity whydahUserIdentity;
	        	
	        	whydahUserIdentity = new WhydahUserIdentity();
	        	whydahUserIdentity.setUid(cleanString(lineArray[USERID]));
	        	whydahUserIdentity.setUsername(cleanString(lineArray[USERNAME]));
	        	whydahUserIdentity.setPassword(cleanString(lineArray[PASSWORD]));
	        	
	            whydahUserIdentity.setFirstName(cleanString(lineArray[FIRSTNAME]));
	            whydahUserIdentity.setLastName(cleanString(lineArray[LASTNAME]));
	            whydahUserIdentity.setEmail(cleanString(lineArray[EMAIL]));
	            whydahUserIdentity.setCellPhone(cleanString(lineArray[CELLPHONE]));
	            whydahUserIdentity.setPersonRef(cleanString(lineArray[PERSONREF]));
	            
	            users.add(whydahUserIdentity);
	        }
			return users;
		
		} catch (IOException ioe) {
			logger.error("Unable to read file {}", userImportSource);
			throw new RuntimeException("Unable to import users from file: " + userImportSource);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
	}

	private static String cleanString(String string) {
		return string==null ? string : string.trim();
	}

	private static void validateLine(String line, String[] lineArray) {
		if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
			throw new RuntimeException("User parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}
