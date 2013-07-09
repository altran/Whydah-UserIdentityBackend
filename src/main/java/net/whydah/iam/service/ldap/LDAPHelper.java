package net.whydah.iam.service.ldap;


import net.whydah.iam.service.domain.WhydahUserIdentity;
import net.whydah.iam.service.helper.StringCleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: totto
 * Date: 1/12/11
 * Time: 1:36 PM
 */
public class LDAPHelper {
    private static final Logger logger = LoggerFactory.getLogger(LDAPHelper.class);

    static final String ATTRIBUTE_NAME_TEMPPWD_SALT = "destinationIndicator";
    /**
     * The OU (organizational unit) to add users to
     */
    private static final String USERS_OU = "ou=users";
    private static final String ATTRIBUTE_NAME_CN = "cn";
    private static final String ATTRIBUTE_NAME_UID = "uid";
    private static final String ATTRIBUTE_NAME_SN = "sn";
    private static final String ATTRIBUTE_NAME_GIVENNAME = "givenName";
    private static final String ATTRIBUTE_NAME_MAIL = "mail";
    private static final String ATTRIBUTE_NAME_MOBILE = "mobile";
    private static final String ATTRIBUTE_NAME_PASSWORD = "userpassword";
    private static final String ATTRIBUTE_NAME_PERSONREF = "employeeNumber";

    private static final StringCleaner stringCleaner = new StringCleaner();

    private final Hashtable<String,String> admenv;
    private final String usernameAttribute;

    private DirContext ctx;
    private boolean connected = false;


    public LDAPHelper(String ldapUrl, String admPrincipal, String admCredentials, String usernameAttribute) {
        admenv = new Hashtable<>(4);
        admenv.put(Context.PROVIDER_URL, ldapUrl);
        admenv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        admenv.put(Context.SECURITY_PRINCIPAL, admPrincipal);
        admenv.put(Context.SECURITY_CREDENTIALS, admCredentials);
        this.usernameAttribute = usernameAttribute;

    }

    public void setUp() {
        try {
            ctx = new InitialDirContext(admenv);
        } catch (NamingException ne) {
            logger.error("NamingException in setUP()" +ne.getLocalizedMessage(), ne);
            connected = false;
            
        } catch (Exception e) {
            logger.error("Exception in setUP()"+e.getLocalizedMessage(), e);
            connected = false;
        }
        connected = true;
    }

    public void addWhydahUserIdentity(WhydahUserIdentity userIdentity) throws NamingException {
        if (!userIdentity.validate()) {
            logger.error("Error validating WhydahUserIdentity: {}", userIdentity);
            return;
        }

        if (!connected) {
            setUp();
        }

        Attributes container = getLdapAttributes(userIdentity);

        // Create the entry
        try {
            String userdn = ATTRIBUTE_NAME_UID + '=' + userIdentity.getUid() + "," + USERS_OU;
            ctx.createSubcontext(userdn, container);
            logger.debug("Added {} with dn={}", userIdentity, userdn);
        } catch (NameAlreadyBoundException nabe) {
            logger.info("User already exist in LDAP: {}", userIdentity);
        } catch (InvalidAttributeValueException iave){
            logger.info("LDAP user with illegal state: {}: {}", userIdentity, iave.getLocalizedMessage());
        }
    }

    /**
     * Schemas: http://www.zytrax.com/books/ldap/ape/
     */
    private Attributes getLdapAttributes(WhydahUserIdentity userIdentity) {
        // Create a container set of attributes
        Attributes container = new BasicAttributes();
        // Create the objectclass to add
        Attribute objClasses = createObjClasses();
        container.put(objClasses);
        container.put(new BasicAttribute(ATTRIBUTE_NAME_CN, userIdentity.getPersonName()));
        container.put(new BasicAttribute(ATTRIBUTE_NAME_GIVENNAME, userIdentity.getFirstName()));
        container.put(new BasicAttribute(ATTRIBUTE_NAME_SN, userIdentity.getLastName()));
        container.put(new BasicAttribute(ATTRIBUTE_NAME_UID, stringCleaner.cleanString(userIdentity.getUid())));
        container.put(new BasicAttribute(ATTRIBUTE_NAME_MAIL, stringCleaner.cleanString(userIdentity.getEmail())));
        container.put(new BasicAttribute(usernameAttribute, stringCleaner.cleanString(userIdentity.getUsername())));
        container.put(new BasicAttribute(ATTRIBUTE_NAME_PASSWORD, userIdentity.getPassword()));

        if (userIdentity.getPersonRef() != null && userIdentity.getPersonRef().length() > 0) {
            container.put(new BasicAttribute(ATTRIBUTE_NAME_PERSONREF, stringCleaner.cleanString(userIdentity.getPersonRef())));
        }

        if (userIdentity.getCellPhone() != null && userIdentity.getCellPhone().length() > 2) {
            container.put(new BasicAttribute(ATTRIBUTE_NAME_MOBILE, stringCleaner.cleanString(userIdentity.getCellPhone())));
        }
        //container.put(new BasicAttribute(ATTRIBUTE_NAME_TEMPPWD_SALT, "TEMPPW"));
        return container;
    }

    public void updateUser(String username, WhydahUserIdentity newuser) {
        if (!newuser.validate()) {
            logger.warn("{} is not valid", newuser);
            return;
        }
        if (!connected) {
            setUp();
        }
        try {
            WhydahUserIdentity olduser = getUserinfo(username);
            if(olduser == null) {
                throw new IllegalArgumentException("User " + username + " not found");
            }
            ArrayList<ModificationItem> modificationItems = new ArrayList<>(7);
            addModificationItem(modificationItems, ATTRIBUTE_NAME_CN, olduser.getPersonName(), newuser.getPersonName());
            addModificationItem(modificationItems, ATTRIBUTE_NAME_GIVENNAME, olduser.getFirstName(), newuser.getFirstName());
            addModificationItem(modificationItems, ATTRIBUTE_NAME_SN, olduser.getLastName(), newuser.getLastName());
            addModificationItem(modificationItems, ATTRIBUTE_NAME_MAIL, olduser.getEmail(), stringCleaner.cleanString(newuser.getEmail()));
            addModificationItem(modificationItems, ATTRIBUTE_NAME_PERSONREF, olduser.getPersonName(), newuser.getPersonName());
            addModificationItem(modificationItems, usernameAttribute, olduser.getUsername(), newuser.getUsername());
            addModificationItem(modificationItems, ATTRIBUTE_NAME_MOBILE, olduser.getCellPhone(), newuser.getCellPhone());

            ctx.modifyAttributes(createUserDNFromUID(newuser.getUid()), modificationItems.toArray(new ModificationItem[modificationItems.size()]));
        } catch (NamingException ne) {
            logger.error(ne.getLocalizedMessage(), ne);
        }
    }

    private void addModificationItem(ArrayList<ModificationItem> modificationItems, String attributeName, String oldValue, String newValue) {
        if((oldValue != null && oldValue.equals(newValue)) || (oldValue == null && newValue == null)) {
            logger.debug("Endrer ikke " + attributeName + "=" + newValue);
        } else if(oldValue == null) {
            logger.debug("Legger til " + attributeName + "=" + newValue);
            modificationItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(attributeName, newValue)));
        } else if(newValue == null) {
            logger.debug("Fjerner til " + attributeName);
            modificationItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attributeName, oldValue)));
        } else {
            logger.debug("Endrer til " + attributeName + "=" + oldValue + " til " + newValue);
            modificationItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attributeName, newValue)));
        }
    }

    private Attribute createObjClasses() {
        Attribute objClasses = new BasicAttribute("objectClass");
        objClasses.add("top");
        objClasses.add("person");
        objClasses.add("organizationalPerson");
        objClasses.add("inetOrgPerson");
        return objClasses;
    }

    private String createUserDN(String username) throws NamingException {
        Attributes attributes = getUserAttributes(username);
        if(attributes == null)
        {
        	logger.debug("Atributes/User are null");
            return null;
        }
        String uid = getAttribValue(attributes, ATTRIBUTE_NAME_UID);
        logger.debug("UID is "+uid);
        return createUserDNFromUID(uid);
    }

    private String createUserDNFromUID(String uid) throws NamingException {
        return new StringBuilder(ATTRIBUTE_NAME_UID).append('=').append(uid).append(",").append(USERS_OU).toString();
    }

    public WhydahUserIdentity getUserinfo(String username) throws NamingException {
        if (!connected) {
            setUp();
        }

        Attributes attributes = getUserAttributes(username);
        if (attributes == null) {
            return null;
        }

        WhydahUserIdentity id = new WhydahUserIdentity();
        id.setUid((String) attributes.get(ATTRIBUTE_NAME_UID).get());
        id.setUsername((String) attributes.get(usernameAttribute).get());
        id.setFirstName(getAttribValue(attributes, ATTRIBUTE_NAME_GIVENNAME));
        id.setLastName(getAttribValue(attributes, ATTRIBUTE_NAME_SN));
        id.setEmail(getAttribValue(attributes, ATTRIBUTE_NAME_MAIL));
        id.setPersonRef(getAttribValue(attributes, ATTRIBUTE_NAME_PERSONREF));
        id.setCellPhone(getAttribValue(attributes, ATTRIBUTE_NAME_MOBILE));
        return id;
    }

    public boolean usernameExist(String username) throws NamingException {
        return getUserinfo(username) != null;
    }


    private Attributes getUserAttributes(String username) throws NamingException {
        logger.debug("getUserAttributes for username=" + username);
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration results = ctx.search("", "(" + usernameAttribute + "=" + username + ")", constraints);
        if (results.hasMore()) {
            SearchResult searchResult = (SearchResult) results.next();
            return searchResult.getAttributes();
        }
        logger.debug("No attributes found for username=" + username);
        return null;
    }

    private String getAttribValue(Attributes attributes, String attributeName) throws NamingException {
        Attribute attribute = attributes.get(attributeName);
        if(attribute != null) {
            return (String) attribute.get();
        } else {
            return null;
        }
    }

    public void deleteUser(String username) {
        try {
            ctx.destroySubcontext(createUserDN(username));
        } catch (NamingException ne) {
            logger.error(ne.getLocalizedMessage(), ne);
        }
    }

    public void changePassword(String username, String newpassword) {
        if (!connected) {
            setUp();
        }
        try {
            Attributes attributes = getUserAttributes(username);
            String userDN = createUserDN(username);
            if (attributes.get(ATTRIBUTE_NAME_TEMPPWD_SALT) != null) {
                ModificationItem mif = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_TEMPPWD_SALT));
                ModificationItem[] mis = {mif};
                ctx.modifyAttributes(userDN, mis);
            }
            ModificationItem mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_PASSWORD, newpassword));
            ModificationItem[] mis = {mi};
            ctx.modifyAttributes(userDN, mis);
        } catch (NamingException ne) {
            logger.error(ne.getLocalizedMessage(), ne);
        }
    }

    public void setTempPassword(String username, String password, String salt) {
        if (!connected) {
            setUp();
        }
        try {
            Attributes attributes = getUserAttributes(username);
            if(getAttribValue(attributes, ATTRIBUTE_NAME_TEMPPWD_SALT) == null) {
                ModificationItem mif = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_TEMPPWD_SALT, salt));
                ModificationItem[] mis = {mif};
                ctx.modifyAttributes(createUserDN(username), mis);
            } else {
                ModificationItem mif = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_TEMPPWD_SALT, salt));
                ModificationItem[] mis = {mif};
                ctx.modifyAttributes(createUserDN(username), mis);
            }
            ModificationItem mip = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_PASSWORD, password));
            ModificationItem[] mis = {mip};
            ctx.modifyAttributes(createUserDN(username), mis);
        } catch (NamingException ne) {
            logger.error(ne.getLocalizedMessage(), ne);
        }
    }

    public String getSalt(String user) {
        try {
            Attributes attributes = getUserAttributes(user);
            return getAttribValue(attributes, ATTRIBUTE_NAME_TEMPPWD_SALT);
        } catch (NamingException ne) {
            logger.error(ne.getLocalizedMessage(), ne);
        }
        return null;
    }

}



