package net.whydah.identity.dataimport;

import net.whydah.identity.application.ApplicationRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.LuceneIndexer;
import net.whydah.identity.util.FileUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IamDataImporterTest {
    private final static String basepath = "target/UserAuthenticationEndpointTest/";
	private static final String lucenePath = basepath + "lucene";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";

    private static EmbeddedADS ads;
    private static LdapUserIdentityDao ldapUserIdentityDao;
    private static UserPropertyAndRoleRepository roleRepository;

	private static OrganizationImporter organizationImporter;
	private static WhydahUserIdentityImporter userImporter;
	private static RoleMappingImporter roleMappingImporter;
	private static ApplicationImporter applicationImporter;
	
    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
    	FileUtils.deleteDirectory(new File(basepath + "/hsqldb"));
        FileUtils.deleteDirectory(new File(lucenePath));

        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        String LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        
        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        boolean readOnly = Boolean.parseBoolean(AppConfig.appConfig.getProperty("ldap.primary.readonly"));
        ldapUserIdentityDao = new LdapUserIdentityDao(LDAP_URL, "uid=admin,ou=system", "secret", "uid", "initials", readOnly);


        roleRepository = new UserPropertyAndRoleRepository();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        QueryRunner queryRunner = new QueryRunner(dataSource);

        new DatabaseMigrationHelper(dataSource).upgradeDatabase();

        roleRepository.setQueryRunner(queryRunner);
        ApplicationRepository configDataRepository = new ApplicationRepository(queryRunner);
        //configDataRepository.setQueryRunner(queryRunner);
        roleRepository.setApplicationRepository(configDataRepository);
        Directory index = new NIOFSDirectory(new File(lucenePath));
        LuceneIndexer luceneIndexer = new LuceneIndexer(index);

        organizationImporter = new OrganizationImporter(queryRunner);
        applicationImporter = new ApplicationImporter(queryRunner);
        userImporter = new WhydahUserIdentityImporter(ldapUserIdentityDao, luceneIndexer);
        roleMappingImporter = new RoleMappingImporter(roleRepository);
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (ads != null) {
            ads.stopServer();
        }
    }
    
    @Test
    public void testDataIsImported() throws Exception {
        
		IamDataImporter iamDataImporter = new IamDataImporter(applicationImporter, organizationImporter, userImporter, roleMappingImporter);
        iamDataImporter.importIamData();
        
        UserIdentity thomaspUserIdentity = ldapUserIdentityDao.getUserIndentity("thomasp");
        assertEquals("Name must be set", "Thomas", thomaspUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Pringle", thomaspUserIdentity.getLastName());
        assertEquals("UserId must be set", "thomas.pringle@altran.com", thomaspUserIdentity.getUid());

        UserIdentity erikdUserIdentity = ldapUserIdentityDao.getUserIndentity("erikd");
        assertEquals("Name must be set", "Erik", erikdUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("UserId must be set", "erik.drolshammer", erikdUserIdentity.getUid());
        
        UserAggregate userAggregate1 = new UserAggregate(thomaspUserIdentity, roleRepository.getUserPropertyAndRoles(thomaspUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles = userAggregate1.getRoles();
        assertEquals("All roles must be found", 3, propsAndRoles.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "thomas.pringle@altran.com", "12", "UserAdminService", "Altran", "developer", "30"));
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "thomas.pringle@altran.com", "15", "SSOLoginWebApplication", "Whydah", "developer", "20"));

        UserAggregate userAggregate2 = new UserAggregate(erikdUserIdentity, roleRepository.getUserPropertyAndRoles(erikdUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles2 = userAggregate2.getRoles();
        assertEquals("All roles must be found", 1, propsAndRoles2.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles2, "erik.drolshammer", "12", "UserAdminService", "Altran", "admin", "70"));

    }

    private boolean containsRoleMapping(List<UserPropertyAndRole> propsAndRoles, String uid, String appId, String appName, String orgName, String roleName, String roleValue) {
        for (UserPropertyAndRole role : propsAndRoles) {
            if(role.getApplicationId().equals(appId) &&
			   role.getApplicationName().equals(appName) && 
			   role.getOrganizationName().equals(orgName) && 
			   role.getApplicationRoleName().equals(roleName) &&
                    role.getApplicationRoleValue().equals(roleValue) &&
                    role.getUid().equals(uid)) {

                return true;
			}
		}
		return false;
	}
}
