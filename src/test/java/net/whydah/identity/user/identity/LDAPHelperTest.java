package net.whydah.identity.user.identity;

import net.whydah.identity.config.AppConfig;
import net.whydah.identity.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.NamingException;
import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

@Ignore
public class LDAPHelperTest {
//    private final static int serverPort = 10363;
    private static String LDAP_URL; // = "ldap://localhost:" + serverPort + "/dc=external,dc=WHYDAH,dc=no";
    private static EmbeddedADS ads;
    private static LDAPHelper ldapHelper; //= new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
    private static LdapAuthenticatorImpl ldapAuthenticator; // = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "uid");

    @BeforeClass
    public static void setUp() throws Exception {
        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        ldapHelper = new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
        ldapAuthenticator = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "uid");
        
        String workDirPath = "target/testldap";
        File workDir = new File(workDirPath);
        FileUtils.deleteDirectory(workDir);
        if (!workDir.mkdirs()) {
            fail("Error creating working directory " + workDirPath);
        }
        // Create the server
        ads = new EmbeddedADS(workDir);
        ads.startServer(LDAP_PORT);
        Thread.sleep(1000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ads.stopServer();
    }

    @Test
    public void addUser() throws NamingException {
        WhydahUserIdentity user = createUser("jan", "Oddvar", "jensen", "staven@hotmail.com", "staven@hotmail.com");
        ldapHelper.addWhydahUserIdentity(user);
        WhydahUserIdentity gotUser = ldapHelper.getUserinfo("jan");
        assertNotNull(gotUser);
    }

    @Test
    public void deleteUser() throws NamingException {
        String uid = UUID.randomUUID().toString();
        String username = "nalle";
        WhydahUserIdentity user = createUser(username, "Trevor", "Treske", "tretre@hotmail.com", uid);
        ldapHelper.addWhydahUserIdentity(user);
        WhydahUserIdentity gotUser = ldapHelper.getUserinfo(user.getUsername());
        //System.out.println("gotUser " + gotUser);
        assertNotNull(gotUser);
        ldapHelper.deleteUser(username);
        WhydahUserIdentity gotUser2 = ldapHelper.getUserinfo(user.getUsername());
        //System.out.println(gotUser2);
        assertNull(gotUser2);
    }

    @Test
    public void changePassword() throws NamingException {
        WhydahUserIdentity user = createUser("stoven@hotmail.com", "Oddvar", "Bra", "stoven@hotmail.com", "stoven@hotmail.com");
        ldapHelper.addWhydahUserIdentity(user);
        assertNotNull(ldapAuthenticator.authenticateWithTemporaryPassword("stoven@hotmail.com", "pass"));
        assertNull(ldapAuthenticator.auth("stoven@hotmail.com", "snafs"));
        ldapHelper.changePassword("stoven@hotmail.com", "snafs");
        assertNull(ldapAuthenticator.auth("stoven@hotmail.com", "pass"));
        assertNotNull(ldapAuthenticator.auth("stoven@hotmail.com", "snafs"));
    }

    @Test
    public void updateUser() throws NamingException {
        String uid = UUID.randomUUID().toString();
        String username = "nalle";
        WhydahUserIdentity user = createUser(username, "Nalle", "Puh", "nalle@hotmail.com", uid);
        ldapHelper.addWhydahUserIdentity(user);
        WhydahUserIdentity gotUser = ldapHelper.getUserinfo(username);
        assertNull(gotUser.getCellPhone());
        gotUser.setCellPhone("32323232");
        ldapHelper.updateUser(username, gotUser);
        WhydahUserIdentity gotUpdatedUser = ldapHelper.getUserinfo(username);
        assertEquals("32323232", gotUpdatedUser.getCellPhone());
        gotUpdatedUser.setCellPhone(null);
        gotUpdatedUser.setFirstName("Emil");
        ldapHelper.updateUser(username, gotUpdatedUser);
        gotUpdatedUser = ldapHelper.getUserinfo(username);
        assertEquals("Emil", gotUpdatedUser.getFirstName());
        assertNull(gotUpdatedUser.getCellPhone());
    }


    private static WhydahUserIdentity createUser(String username, String firstName, String lastName, String email, String uid) {
        WhydahUserIdentity userIdentity = new WhydahUserIdentity();
        userIdentity.setUsername(username);
        userIdentity.setFirstName(firstName);
        userIdentity.setLastName(lastName);
        userIdentity.setEmail(email);
        userIdentity.setUid(uid);
        userIdentity.setPersonRef("r" + uid);
        userIdentity.setPassword("pass");
        return userIdentity;
    }
}