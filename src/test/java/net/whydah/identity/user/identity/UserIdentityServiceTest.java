package net.whydah.identity.user.identity;

import com.sun.jersey.api.ConflictException;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.user.search.LuceneSearch;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
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
    private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao;
    private static PasswordGenerator passwordGenerator;
    private static LuceneIndexer luceneIndexer;

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        int LDAP_PORT = 19389;
        String ldapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        boolean readOnly = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly"));
        ldapUserIdentityDao = new LdapUserIdentityDao(ldapUrl, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);

        String workDirPath = "target/" + UserIdentityServiceTest.class.getSimpleName();
        File workDir = new File(workDirPath);
        FileUtils.deleteDirectory(workDir);
        if (!workDir.mkdirs()) {
            fail("Error creating working directory " + workDirPath);

        }

        Directory index = new RAMDirectory();

        luceneIndexer = new LuceneIndexer(index);

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
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, null, luceneIndexer, Mockito.mock(LuceneSearch.class));

        String username = "username123";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "personRef",
                "test@test.no", "12345678", "password");
        userIdentityService.addUserIdentity(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIndentity(username);

        assertEquals(userIdentity, fromLdap);

        userIdentityService.addUserIdentity(userIdentity);
        //.addUserIdentity(userIdentity);
        fail("Expected ConflictException because user should already exist.");
    }
}
