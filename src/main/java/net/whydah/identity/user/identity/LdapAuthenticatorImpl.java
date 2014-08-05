package net.whydah.identity.user.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP authentication.
 */
public class LdapAuthenticatorImpl {
    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticatorImpl.class);

    private final String usernameAttribute;
    private final Hashtable<String,String> baseenv;
    private final Hashtable<String,String> admenv;

    public LdapAuthenticatorImpl(String ldapUrl, String admPrincipal, String admCredentials, String usernameAttribute) {
        log.info("Initialize LdapAuthenticatorImpl with ldapUrl={}, admPrincipal={}, usernameAttribute={}", ldapUrl, admPrincipal, usernameAttribute);
        baseenv = new Hashtable<>();
        baseenv.put(Context.PROVIDER_URL, ldapUrl);
        baseenv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        this.admenv = new Hashtable<>(baseenv);
        admenv.put(Context.SECURITY_PRINCIPAL, admPrincipal);
        admenv.put(Context.SECURITY_CREDENTIALS, admCredentials);
        this.usernameAttribute = usernameAttribute;
    }


    public UserIdentity authenticate(final String username, final String password) {
        InitialDirContext initialDirContext = authenticateUser(username, password);
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
     * @return  a authenticated UserIdentity
     */
    private InitialDirContext authenticateUser(final String username, final String password) {
        log.trace("authenticateUser with username and password. username=" + username);
        if (username == null || password == null) {
            log.info("authenticateUser failed (returned null) because password or username was null.");
            return null;
        }

        final String userDN = findUserDN(username);

        Hashtable<String,String> myEnv = new Hashtable<>(baseenv);
        myEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        myEnv.put(Context.SECURITY_PRINCIPAL, userDN);
        myEnv.put(Context.SECURITY_CREDENTIALS, password);

        try {
            return new InitialDirContext(myEnv);
        } catch (Exception e) {
            log.error("Failed to create InitialDirContext in authenticate.", e);
            return null;
        }
    }


    public boolean authenticateWithTemporaryPassword(String username, String password) {
        InitialDirContext initialDirContext = authenticateUser(username, password);
        return initialDirContext != null;
    }


    private String findUserDN(String username) {
        InitialDirContext adminContext;
        try {
            adminContext = new InitialDirContext(admenv);
        } catch (Exception e) {
            log.error("Error authenticating as superuser, check configuration", e);
            return null;
        }

        try {
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration results = adminContext.search("", "(" + usernameAttribute + "=" + username + ")", constraints);
            if (!results.hasMore()) {
                return null;
            }

            SearchResult searchResult =(SearchResult) results.next();
            String userDN = searchResult.getNameInNamespace();
            if (userDN == null) {
                log.debug("userDN not found for {}", username);
                return null;
            }
            log.debug("findUserDN with username={} found userDN={}", username, userDN);
        } catch (Exception e) {
            log.info("findUserDN failed for user with username=" + username, e);
        }
        return null;
    }


    private UserIdentity getUserinfo(String username, InitialDirContext context) throws NamingException {
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration results = context.search("", "(" + usernameAttribute + "=" + username + ")", constraints);
        // NamingEnumeration results =context.search(GROUPS_OU, filter, cons);
        if (!results.hasMore()) {
            return null;
        }

        SearchResult searchResult = (SearchResult) results.next();
        Attributes attributes = searchResult.getAttributes();
        if (attributes.get(LDAPHelper.ATTRIBUTE_NAME_TEMPPWD_SALT) != null) {
            log.info("User has temp password, must change before logon");
            return null;
        }

        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUid((String) attributes.get("uid").get());
        userIdentity.setUsername((String) attributes.get(usernameAttribute).get());
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



