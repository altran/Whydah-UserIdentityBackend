package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.sso.user.types.UserIdentity;

import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
    private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final LuceneUserSearch luceneSearch;
    private final LuceneUserIndexer luceneIndexer;
    private final boolean alwayslookupinexternaldirectory;

    @Autowired
    @Configure
    public UserSearch(LdapUserIdentityDao ldapUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer, @Configuration("ldap.primary.alwayslookupinexternaldirectory") boolean _alwayslookupinexternaldirectory) {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.luceneSearch = luceneSearch;
        this.luceneIndexer = luceneIndexer;
        this.alwayslookupinexternaldirectory = _alwayslookupinexternaldirectory;
        
    }

    public List<UserIdentity> search(String query) {
        List<UserIdentity> users = luceneSearch.search(query);
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
                    //luceneIndexer.update(user);
                    luceneIndexer.addToIndex(user);
                }
            } catch (NamingException e) {
                log.warn("Could not find users from ldap/AD. Query: {}", query, e);
            }
        }
        return users;
    }
    
    public UserIdentity getUserIdentityIfExists(String username) {
        UserIdentity user = luceneSearch.getUserIdentityIfExists(username);     
        if(user== null && alwayslookupinexternaldirectory){

            try {
                user = ldapUserIdentityDao.getUserIndentity(username); //???
            } catch (NamingException e) {
                log.warn("Could not find username from ldap/AD. Query: {}", username, e);
            }
        }
        return user;
    }
    
    public boolean isUserIdentityIfExists(String username) throws NamingException {
    	boolean existing = luceneSearch.usernameExists(username);
    	if(!existing && alwayslookupinexternaldirectory){
    		return ldapUserIdentityDao.usernameExist(username) ;
    	}
    	return existing;
    	
    }

    public PaginatedUserIdentityDataList query(int page, String query) {
        PaginatedUserIdentityDataList paginatedDL = luceneSearch.query(page, query);
        List<UserIdentity> users = paginatedDL.data;
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
        if (users.isEmpty() && alwayslookupinexternaldirectory) {
            try {
                UserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
                if (user != null) {
                    users.add(user);
                    //Update user to lucene.
                    log.trace("Added a user found in LDAP to lucene index: {}", user.toString());
                    //luceneIndexer.update(user);
                    luceneIndexer.addToIndex(user);
                }
            } catch (NamingException e) {
                log.warn("Could not find users from ldap/AD. Query: {}", query, e);
            }
            
            paginatedDL.data =  users;
        }
       
        return paginatedDL;
    }
}
