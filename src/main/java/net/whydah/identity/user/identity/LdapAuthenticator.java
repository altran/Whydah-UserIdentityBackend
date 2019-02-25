package net.whydah.identity.user.identity;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP authentication.
 */
@Service
public class LdapAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticator.class);
    private final Hashtable<String,String> baseenv;
    private final Hashtable<String,String> admenv;
    private final String uidAttribute;
    private final String usernameAttribute;
    private boolean aBoolean;
    //private static final String uidAttributeForActiveDirectory = "userprincipalname";

    @Autowired
    @Configure
    public LdapAuthenticator(@Configuration("ldap.primary.url") String ldapUrl,
                             @Configuration("ldap.primary.admin.principal") String admPrincipal,
                             @Configuration("ldap.primary.admin.credentials") String admCredentials,
                             @Configuration("ldap.primary.uid.attribute") String uidAttribute,
                             @Configuration("ldap.primary.username.attribute") String usernameAttribute) {

        log.info("Initialize LdapAuthenticator with ldapUrl={}, admPrincipal={}, uidAttribute={}, usernameAttribute={}", ldapUrl, admPrincipal, uidAttribute, usernameAttribute);
        baseenv = new Hashtable<>();
        baseenv.put(Context.PROVIDER_URL, ldapUrl);
        baseenv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        this.admenv = new Hashtable<>(baseenv);
        admenv.put(Context.SECURITY_PRINCIPAL, admPrincipal);
        admenv.put(Context.SECURITY_CREDENTIALS, admCredentials);

        this.uidAttribute = uidAttribute;
        this.usernameAttribute = usernameAttribute;
    }


    public LDAPUserIdentity authenticate(final String username, final String password) {      
    	 InitialDirContext initialDirContext = authenticateUser(username, password, "simple");
         if (initialDirContext == null) {
             return null;
         }
    	try {
            return getUserinfo(username, initialDirContext);
        } catch (NamingException e) {
            log.error("Failed to create getUserinfo in authenticate.", e);
        }
        return null;
    }

    /**
     * Authenticate with LDAP using "simple" authentication (username and password).
     *
     * Resources:
     * http://docs.oracle.com/javase/jndi/tutorial/ldap/security/ldap.html
     * http://docs.oracle.com/javase/tutorial/jndi/ldap/authentication.html
     * http://docs.oracle.com/javase/tutorial/jndi/ldap/auth_mechs.html
     *
     * @param username  username
     * @param password  user password
     * @param securityAuthenticationLevel
     * @return a authenticated LDAPUserIdentity
     */
    private InitialDirContext authenticateUser(final String username, final String password, String securityAuthenticationLevel) {
        if (username == null || password == null) {
            log.debug("authenticateUser failed (returning null), because password or username was null.");
            return null;
        }

        final String userDN = findUserDN(username);
        if (userDN == null) {
            log.debug("authenticateUser failed (returned null), because could not find userDN for username={}", username);
            return null;
        }

        Hashtable<String,String> myEnv = new Hashtable<>(baseenv);
        if (securityAuthenticationLevel.equals("simple")) {
            myEnv.put(Context.SECURITY_AUTHENTICATION, securityAuthenticationLevel);
        }
        myEnv.put(Context.SECURITY_PRINCIPAL, userDN);
        myEnv.put(Context.SECURITY_CREDENTIALS, password);

        try {
            InitialDirContext initialDirContext = new InitialDirContext(myEnv);
            log.debug("authenticateUser with username and password was successful for username=" + username);
            return initialDirContext;
        } catch (AuthenticationException ae) {
            log.debug("authenticateUser failed (returned null), because {}: {}", ae.getClass().getSimpleName(), ae.getMessage());
        } catch (Exception e) {
            log.error("authenticateUser failed (returned null), because could not create InitialDirContext.", e);
            return null;
        }
        return null;

    }


    public boolean authenticateWithTemporaryPassword(String username, String password) {
        InitialDirContext initialDirContext = authenticateUser(username, password, "none");
        return initialDirContext != null;
    }


    private String findUserDN(String username) {
        
        try {
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String baseDN = "";
            String filter = "(" + usernameAttribute + "=" + username + ")";
            SearchResult searchResult;
            try {
                searchResult = new CommandLdapSearch(getContext(), baseDN, filter, constraints).execute();
            } catch (HystrixBadRequestException he) {
                if (he.getCause() instanceof NamingException) {
                    throw (NamingException) he.getCause();
                }
                throw he;
            }
            if (searchResult == null) {
                log.debug("findUserDN, empty searchResult for {}={}", usernameAttribute, username);
                return null;
            }
            String userDN = searchResult.getNameInNamespace();
            if (userDN == null) {
                log.debug("findUserDN, userDN not found for {}={}", usernameAttribute, username);
                return null;
            }
            log.debug("findUserDN with {}={} found userDN={}", usernameAttribute, username, userDN);
            return userDN;
        } catch (Exception e) {
            log.debug("findUserDN failed for user with usernameattribute=username: {}={}, ",usernameAttribute, username, e);
            return null;
        }
    }


	private InitialDirContext getContext() throws NamingException {
		return new InitialDirContext(admenv);    
	}


    private LDAPUserIdentity getUserinfo(String username, InitialDirContext context) throws NamingException {
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String baseDN = "";
        String filter = "(" + usernameAttribute + "=" + username + ")";
        SearchResult searchResult;
        try {
            searchResult = new CommandLdapSearch(context, baseDN, filter, constraints).execute();
        } catch (HystrixBadRequestException he) {
            if (he.getCause() instanceof NamingException) {
                throw (NamingException) he.getCause();
            }
            throw he;
        }
        Attributes attributes = searchResult.getAttributes();
        //HUY: No security concern here b/c user is already authenticated 
        //Huy: should allow here. There is a case in which someone tries to reset my password. I should still be able to access my page
//        if (attributes.get(LdapUserIdentityDao.ATTRIBUTE_NAME_TEMPPWD_SALT) != null) {
//            log.info("User has temp password, must change before logon");
//            return null;
//        }

        LDAPUserIdentity userIdentity = new LDAPUserIdentity();
        userIdentity.setUid(getAttribValue(attributes, uidAttribute));
        userIdentity.setUsername(getAttribValue(attributes, usernameAttribute));
        userIdentity.setFirstName(getAttribValue(attributes, "givenName"));
        userIdentity.setLastName(getAttribValue(attributes, "sn"));
        userIdentity.setEmail(getAttribValue(attributes, "mail"));
        userIdentity.setPersonRef(getAttribValue(attributes, "employeeNumber"));
        userIdentity.setCellPhone(getAttribValue(attributes, "mobile"));
        return userIdentity;
    }
    private String getAttribValue(Attributes attributes, String attributeName) throws NamingException {
        Attribute attribute = attributes.get(attributeName);
        if (attribute == null) {
            return null;
        }
        return (String) attribute.get();
    }
}



