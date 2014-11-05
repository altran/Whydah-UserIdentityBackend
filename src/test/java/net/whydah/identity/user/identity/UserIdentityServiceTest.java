package net.whydah.identity.user.identity;

import net.whydah.identity.application.ApplicationRepository;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.user.search.LuceneSearch;
import net.whydah.identity.util.FileUtils;
import net.whydah.identity.util.PasswordGenerator;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.File;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 02/04/14
 */
public class UserIdentityServiceTest {
    private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao;
    private static PasswordGenerator passwordGenerator;
    private static UserPropertyAndRoleRepository roleRepository;
    private static QueryRunner queryRunner;


    private static LuceneIndexer luceneIndexer;
    private static UserAdminHelper userAdminHelper;

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        int LDAP_PORT = 19389;
        String ldapUrl = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        boolean readOnly = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly"));
        ldapUserIdentityDao = new LdapUserIdentityDao(ldapUrl, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + "hsqldbtest");
        queryRunner = new QueryRunner(dataSource);
        AuditLogRepository auditLogRepository = new AuditLogRepository(queryRunner);

        ApplicationRepository configDataRepository = new ApplicationRepository(queryRunner);
        roleRepository = new UserPropertyAndRoleRepository();

        roleRepository.setApplicationRepository(configDataRepository);
        roleRepository.setQueryRunner(queryRunner);

        Directory index = new NIOFSDirectory(new File("lucene"));

        userAdminHelper = new UserAdminHelper(ldapUserIdentityDao, new LuceneIndexer(index), auditLogRepository, roleRepository);

        String workDirPath = "target/" + UserIdentityServiceTest.class.getSimpleName();
        File workDir = new File(workDirPath);
        FileUtils.deleteDirectory(workDir);
        if (!workDir.mkdirs()) {
            fail("Error creating working directory " + workDirPath);

        }


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

    @Test
    public void testAddUserToLdap() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, null, luceneIndexer, Mockito.mock(LuceneSearch.class));

        String username = "username123";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "personRef",
                "test@test.no", "12345678", "password");
        userAdminHelper.addUser(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertTrue("Expected ConflictException because user should already exist.", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }


    @Test
    public void testAddUserStrangeCellPhone() throws Exception {
        UserIdentityService userIdentityService =
                new UserIdentityService(null, ldapUserIdentityDao, null, passwordGenerator, null, luceneIndexer, Mockito.mock(LuceneSearch.class));

        String username = "username1234";
        UserIdentity userIdentity = new UserIdentity("uid2", username, "firstName2", "lastName2", "personRef2",
                "test2@test.no", "+47 123 45 678", "password2");
        userAdminHelper.addUser(userIdentity);

        UserIdentityRepresentation fromLdap = userIdentityService.getUserIdentity(username);

        assertEquals(userIdentity, fromLdap);
        Response response = userAdminHelper.addUser(userIdentity);
        assertTrue("Expected ConflictException because user should already exist.", response.getStatus() == Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }
}
