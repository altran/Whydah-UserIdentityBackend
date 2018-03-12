package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.sso.user.types.UserIdentity;
import net.whydah.sso.util.Lock;

import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
    private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final LuceneUserSearch luceneUserSearch;
    private final LuceneUserIndexer luceneUserIndexer;
    private final boolean alwayslookupinexternaldirectory;
    ScheduledExecutorService scheduler;
    Lock locker = new Lock();

    @Autowired
    @Configure
    public UserSearch(LdapUserIdentityDao ldapUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer, @Configuration("ldap.primary.alwayslookupinexternaldirectory") boolean _alwayslookupinexternaldirectory) {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.luceneUserSearch = luceneSearch;
        this.luceneUserIndexer = luceneIndexer;
        this.alwayslookupinexternaldirectory = _alwayslookupinexternaldirectory;
        //start a thread to populate data from LDAP to user index list
        scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						try{
							
							if(!locker.isLocked()){
								locker.lock();
								if(luceneUserSearch.getUserIndexSize()==0 && alwayslookupinexternaldirectory){
									
									log.debug("lucene index is empty. Trying to import from LDAP...");
									
									List<LDAPUserIdentity> list = ldapUserIdentityDao.getAllUsers();

						    		log.debug("Found LDAP user list size: {}", list.size());
						    		
									for(LDAPUserIdentity user : list){
										 log.debug("Added a user found in LDAP to lucene index: {}", user.toString());
										 luceneUserIndexer.addToIndex(user);
									}
								}
							}
							
														
						} catch(Exception ex){
							ex.printStackTrace();
						} finally{
							locker.unlock();
						}
					}
				},
				1, 1, TimeUnit.MINUTES);
 
    }

    public List<UserIdentity> search(String query) {
        List<UserIdentity> users = luceneUserSearch.search(query);
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
        if (users.isEmpty() && alwayslookupinexternaldirectory) {
            try {
                LDAPUserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
                if (user != null) {
                    users.add(user);
                    //Update user to lucene.
                    log.trace("Added a user found in LDAP to lucene index: {}", user.toString());
                    //luceneUserIndexer.update(user);
                    luceneUserIndexer.addToIndex(user);
                }
            } catch (NamingException e) {
                log.warn("Could not find users from ldap/AD. Query: {}", query, e);
            }
        }
        return users;
    }

    public UserIdentity getUserIdentityIfExists(String username) {
        UserIdentity user = luceneUserSearch.getUserIdentityIfExists(username);
        if (user == null && alwayslookupinexternaldirectory) {

            try {
                user = ldapUserIdentityDao.getUserIndentity(username); //???
            } catch (NamingException e) {
                log.warn("Could not find username from ldap/AD. Query: {}", username, e);
            }
        }
        return user;
    }

    public boolean isUserIdentityIfExists(String username) throws NamingException {
        boolean existing = luceneUserSearch.usernameExists(username);
        if (!existing && alwayslookupinexternaldirectory) {
            return ldapUserIdentityDao.usernameExist(username);
        }
        return existing;

    }

    public PaginatedUserIdentityDataList query(int page, String query) {
        PaginatedUserIdentityDataList paginatedDL = luceneUserSearch.query(page, query);
        List<UserIdentity> users = paginatedDL.data;
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
//        if (users.isEmpty() && alwayslookupinexternaldirectory) {
//            try {
//                UserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
//                if (user != null) {
//                    users.add(user);
//                    //Update user to lucene.
//                    log.trace("Added a user found in LDAP to lucene index: {}", user.toString());
//                    //luceneUserIndexer.update(user);
//                    luceneUserIndexer.addToIndex(user);
//                }
//            } catch (NamingException e) {
//                log.warn("Could not find users from ldap/AD. Query: {}", query, e);
//            }
//
//            paginatedDL.data = users;
//        }

        return paginatedDL;
    }

    public int getUserIndexSize() {
        return luceneUserSearch.getUserIndexSize();
    }
}
