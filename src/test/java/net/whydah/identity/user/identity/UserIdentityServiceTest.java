package net.whydah.identity.user.identity;

import com.sun.jersey.api.ConflictException;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.search.Search;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 02/04/14
 */
public class UserIdentityServiceTest {
    private static String ldapUrl; // = "ldap://localhost:" + serverPort + "/dc=external,dc=WHYDAH,dc=no";
    private static EmbeddedADS ads;
    private static LDAPHelper ldapHelper; //= new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
    private static LdapAuthenticatorImpl ldapAuthenticator; // = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "uid");
    private static PasswordGenerator passwordGenerator;


    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        //int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        int LDAP_PORT = 19389;
        ldapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        ldapHelper = new LDAPHelper(ldapUrl, "uid=admin,ou=system", "secret", "initials");
        ldapAuthenticator = new LdapAuthenticatorImpl(ldapUrl, "uid=admin,ou=system", "secret", "uid");

        String workDirPath = "target/" + UserIdentityServiceTest.class.getSimpleName();
        File workDir = new File(workDirPath);
        FileUtils.deleteDirectory(workDir);
        if (!workDir.mkdirs()) {
            fail("Error creating working directory " + workDirPath);
        }
        // Create the server
        ads = new EmbeddedADS(workDir);
        ads.startServer(LDAP_PORT);
        Thread.sleep(1000);

        passwordGenerator = new PasswordGenerator();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ads.stopServer();
    }

    @Test(expected = ConflictException.class)
    public void testAddUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapHelper, null, passwordGenerator, null, null, Mockito.mock(Search.class));

        String username = "username123";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "personRef",
                "email", "12345678", "password");
        userIdentityService.addUserIdentity(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIndentity(username);

        assertEquals(userIdentity, fromLdap);

        userIdentityService.addUserIdentity(userIdentity);
        fail("Expected ConflictException because user should already exist.");
    }
}
