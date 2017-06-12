package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UIBUserIdentity;
import net.whydah.identity.user.identity.UIBUserIdentityRepresentation;
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

    @Autowired
    public UserSearch(LdapUserIdentityDao ldapUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer) {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.luceneSearch = luceneSearch;
        this.luceneIndexer = luceneIndexer;
    }

    public List<UIBUserIdentityRepresentation> search(String query) {
        List<UIBUserIdentityRepresentation> users = luceneSearch.search(query);
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
        if (users.isEmpty()) {
            try {
                UIBUserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
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
    
    public PaginatedUIBUserIdentityDataList query(int page, String query) {
    	PaginatedUIBUserIdentityDataList paginatedDL = luceneSearch.query(page, query);
        List<UIBUserIdentity> users = paginatedDL.data;
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
        if (users.isEmpty()) {
            try {
                UIBUserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
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
