package net.whydah.identity.user.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP-autentisering.
 */
public class LdapAuthenticatorImpl {
    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticatorImpl.class);

    private final String usernameAttribute;
    private final Hashtable<String,String> baseenv;
    private final Hashtable<String,String> admenv;


    public LdapAuthenticatorImpl(String ldapUrl, String admPrincipal, String admCredentials, String usernameAttribute) {
        log.debug("Initialize LdapAuthenticatorImpl with ldapUrl=" + ldapUrl + ", admPrincipal=" + admPrincipal +
                ", admCredentials=" + admCredentials + ", usernameAttribute=" + usernameAttribute);
        baseenv = new Hashtable<>();
        baseenv.put(Context.PROVIDER_URL, ldapUrl);
        baseenv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        this.admenv = new Hashtable<>(baseenv);
        admenv.put(Context.SECURITY_PRINCIPAL, admPrincipal);
        admenv.put(Context.SECURITY_CREDENTIALS, admCredentials);
        this.usernameAttribute = usernameAttribute;
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
    public UserIdentity authenticate(final String username, final String password) {
        log.debug("Trying to authenticate with username and password. username=" + username);

        if (username == null || password == null) {
            return null;
        }

        try {
            final String userDN = findUserDN(username);
            if (userDN == null) {
                log.info("userDN not found for {}", username);
                return null;
            }
            log.debug("Found userDN=" + userDN);

            Hashtable<String,String> myEnv = new Hashtable<>(baseenv);
            myEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            myEnv.put(Context.SECURITY_PRINCIPAL, userDN);
            myEnv.put(Context.SECURITY_CREDENTIALS, password);
            InitialDirContext context = new InitialDirContext(myEnv);
            UserIdentity userIdentity = getUserinfo(username, context);
            return userIdentity;
        } catch (Exception e) {
            log.debug("Authentication failed for user " + username, e);
            return null;
        }
    }

    public boolean authenticateWithTemporaryPassword(String username, String password) {
        try {
            final String userDN = findUserDN(username);
            if (userDN == null) {
                log.info("userDN not found for {}", username);
                return false;
            }
            log.debug("Found userND=" + userDN);
            Hashtable<String,String> myEnv = new Hashtable<>(baseenv);
            myEnv.put(Context.SECURITY_PRINCIPAL, userDN);
            myEnv.put(Context.SECURITY_CREDENTIALS, password);
            new InitialDirContext(myEnv);
        } catch (AuthenticationException e) {
            log.info("Authentication failed for user {}", username);
            return false;
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }


    private String findUserDN(String username) throws NamingException {
        InitialDirContext context;
        try {
            context = new InitialDirContext(admenv);
        } catch (AuthenticationException e) {
            log.error("Error authenticating as superuser, check configuration", e);
            throw e;
        }
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration results = context.search("", "(" + usernameAttribute + "=" + username + ")", constraints);
        if (!results.hasMore()) {
            return null;
        }

        SearchResult searchResult =(SearchResult) results.next();
        return searchResult.getNameInNamespace();
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



