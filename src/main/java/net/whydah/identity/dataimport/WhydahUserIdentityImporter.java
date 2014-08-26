package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
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
	private static final Logger log = LoggerFactory.getLogger(WhydahUserIdentityImporter.class);

	private static final int REQUIRED_NUMBER_OF_FIELDS = 8;
	private static final int USERID = 0;
	private static final int USERNAME = 1;
	private static final int PASSWORD = 2;
	private static final int FIRSTNAME = 3;
	private static final int LASTNAME = 4;
	private static final int EMAIL = 5;
	private static final int CELLPHONE = 6;
	private static final int PERSONREF = 7;
	
    private LdapUserIdentityDao ldapUserIdentityDao;
    private Directory index;
    
    @Inject
	public WhydahUserIdentityImporter(LdapUserIdentityDao ldapUserIdentityDao, Directory index) {
		this.ldapUserIdentityDao = ldapUserIdentityDao;
		this.index = index;
	}
    
    public void importUsers(String userImportSource) {
        if (userImportSource == null || userImportSource.isEmpty()) {
            log.info("userImportSource was empty, skipping user import.");
            return;
        }

        log.info("importUsers from userImportSource={}", userImportSource);
        List<UserIdentity> users = parseUsers(userImportSource);
    	saveUsers(users);
        log.info("{} users imported.", users.size());
    }

	protected static List<UserIdentity> parseUsers(String userImportSource) {
		BufferedReader reader = null;
		try {
			List<UserIdentity> users = new ArrayList<>();
			InputStream classpathStream = WhydahUserIdentityImporter.class.getClassLoader().getResourceAsStream(userImportSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line;
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	        	UserIdentity userIdentity;
	        	userIdentity = new UserIdentity();
	        	userIdentity.setUid(cleanString(lineArray[USERID]));
	        	userIdentity.setUsername(cleanString(lineArray[USERNAME]));
	        	userIdentity.setPassword(cleanString(lineArray[PASSWORD]));
	            userIdentity.setFirstName(cleanString(lineArray[FIRSTNAME]));
	            userIdentity.setLastName(cleanString(lineArray[LASTNAME]));
	            userIdentity.setEmail(cleanString(lineArray[EMAIL]));
	            userIdentity.setCellPhone(cleanString(lineArray[CELLPHONE]));
	            userIdentity.setPersonRef(cleanString(lineArray[PERSONREF]));
	            
	            users.add(userIdentity);
	        }
			return users;
		
		} catch (IOException ioe) {
			log.error("Unable to read file {}", userImportSource);
			throw new RuntimeException("Unable to import users from file: " + userImportSource);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing stream", e);
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

    private void saveUsers(List<UserIdentity> users) {
        try {
            Indexer indexer = new Indexer(index);
            final IndexWriter indexWriter = indexer.getWriter();
            for (UserIdentity userIdentity : users) {
                ldapUserIdentityDao.addUserIdentity(userIdentity);
                log.info("Imported user. Uid: {}, Name {} {}, Email {}", userIdentity.getUid(), userIdentity.getFirstName(), userIdentity.getLastName(),userIdentity.getEmail());
                indexer.addToIndex(indexWriter, userIdentity);
            }
            indexWriter.optimize();
            indexWriter.close();
        } catch (Exception e) {
            log.error("Error importing users!", e);
            throw new RuntimeException("Error importing users!", e);
        }
    }
}
