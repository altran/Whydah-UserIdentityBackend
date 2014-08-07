package net.whydah.identity.user.identity;

import net.whydah.identity.config.AppConfig;
import net.whydah.identity.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

public class LdapUserIdentityDaoTest {
    private static String ldapUrl; // = "ldap://localhost:" + serverPort + "/dc=external,dc=WHYDAH,dc=no";
    private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao; //= new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
    private static LdapAuthenticator ldapAuthenticator; // = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "uid");

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);

        //int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        int LDAP_PORT = 18389;
        ldapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        ldapUserIdentityDao = new LdapUserIdentityDao(ldapUrl, "uid=admin,ou=system", "secret", "initials",Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly")));
        ldapAuthenticator = new LdapAuthenticator(ldapUrl, "uid=admin,ou=system", "secret", "uid");

        String workDirPath = "target/" + LdapUserIdentityDaoTest.class.getSimpleName();
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
        if (ads != null) {
            ads.stopServer();
        }
    }

    @Test
    public void testAddUser() throws Exception {
        UserIdentity user = createUser("jan", "Oddvar", "jensen", "staven@hotmail.com", "staven@hotmail.com", "pass");
        ldapUserIdentityDao.addUserIdentity(user);
        UserIdentityRepresentation gotUser = ldapUserIdentityDao.getUserIndentity("jan");
        assertNotNull(gotUser);
    }

    @Test
    public void testUpdateUser() throws Exception {
        String uid = UUID.randomUUID().toString();
        String username = "nalle";
        UserIdentity user = createUser(username, "Nalle", "Puh", "nalle@hotmail.com", uid, "pass");
        ldapUserIdentityDao.addUserIdentity(user);
        UserIdentity gotUser = ldapUserIdentityDao.getUserIndentity(username);
        assertNull(gotUser.getCellPhone());

        String cellPhone = "32323232";
        gotUser.setCellPhone(cellPhone);
        ldapUserIdentityDao.updateUserIdentityForUsername(username, gotUser);
        UserIdentity gotUpdatedUser = ldapUserIdentityDao.getUserIndentity(username);
        assertEquals(cellPhone, gotUpdatedUser.getCellPhone());

        gotUpdatedUser.setCellPhone(null);
        String firstName = "Emil";
        gotUpdatedUser.setFirstName(firstName);
        ldapUserIdentityDao.updateUserIdentityForUsername(username, gotUpdatedUser);
        gotUpdatedUser = ldapUserIdentityDao.getUserIndentity(username);
        assertEquals(firstName, gotUpdatedUser.getFirstName());
        assertNull(gotUpdatedUser.getCellPhone());
    }

    @Test
    public void testDeleteUser() throws Exception {
        String uid = UUID.randomUUID().toString();
        String username = "usernameToBeDeleted";
        UserIdentity user = createUser(username, "Trevor", "Treske", "tretre@hotmail.com", uid, "pass");
        ldapUserIdentityDao.addUserIdentity(user);
        UserIdentityRepresentation gotUser = ldapUserIdentityDao.getUserIndentity(user.getUsername());
        assertNotNull(gotUser);

        boolean deleteSuccessful = ldapUserIdentityDao.deleteUserIdentity(username);
        assertTrue(deleteSuccessful);

        //Thread.sleep(3000);

        UserIdentityRepresentation gotUser2 = ldapUserIdentityDao.getUserIndentity(username);
        assertNull("Expected user to be deleted. " + (gotUser2 != null ? gotUser2.toString() : "null"), gotUser2);
    }

    @Test
    public void testChangePassword() throws Exception {
        String username = "stoven@hotmail.com";
        String firstPassword = "pass";
        String uid = username;
        UserIdentity user = createUser(username, "Oddvar", "Bra", "stoven@hotmail.com", uid, firstPassword);
        ldapUserIdentityDao.addUserIdentity(user);

        assertNotNull(ldapAuthenticator.authenticateWithTemporaryPassword(username, firstPassword));
        String secondPassword = "snafs";
        assertNull(ldapAuthenticator.authenticate(username, secondPassword));

        ldapUserIdentityDao.changePassword(username, secondPassword);
        assertNull(ldapAuthenticator.authenticate(username, firstPassword));
        assertNotNull(ldapAuthenticator.authenticate(username, secondPassword));
    }

    private static UserIdentity createUser(String username, String firstName, String lastName, String email, String uid, String password) {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUsername(username);
        userIdentity.setFirstName(firstName);
        userIdentity.setLastName(lastName);
        userIdentity.setEmail(email);
        userIdentity.setUid(uid);
        userIdentity.setPersonRef("r" + uid);
        userIdentity.setPassword(password);
        return userIdentity;
    }
}