package net.whydah.identity.dataimport;

import net.whydah.identity.application.role.ApplicationRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.identity.EmbeddedADS;
import net.whydah.identity.user.identity.LDAPHelper;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
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

    private final static String basepath = "target/UserTokenResourceTest/";
	private static final String lucenePath = basepath + "lucene";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private static String LDAP_URL; 

    private static EmbeddedADS ads;
    private static LDAPHelper ldapHelper;
    private static UserPropertyAndRoleRepository roleRepository;
    private static QueryRunner queryRunner;
    private static DatabaseHelper databaseHelper;
    


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
        LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";
        
        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        EmbeddedADS ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        ldapHelper = new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");


        roleRepository = new UserPropertyAndRoleRepository();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        queryRunner = new QueryRunner(dataSource);

        databaseHelper = new DatabaseHelper(queryRunner);
        databaseHelper.initDB(DatabaseHelper.DB_DIALECT.HSSQL);

        roleRepository.setQueryRunner(queryRunner);
        ApplicationRepository configDataRepository = new ApplicationRepository();
        configDataRepository.setQueryRunner(queryRunner);
        roleRepository.setApplicationRepository(configDataRepository);
        Directory index = new NIOFSDirectory(new File(lucenePath));

        organizationImporter = new OrganizationImporter(queryRunner);
        applicationImporter = new ApplicationImporter(queryRunner);
        userImporter = new WhydahUserIdentityImporter(ldapHelper, index);
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
        
		IamDataImporter iamDataImporter = new IamDataImporter(databaseHelper, applicationImporter, organizationImporter, userImporter, roleMappingImporter);
        iamDataImporter.importIamData();
        
        WhydahUserIdentity thomaspUserIdentity = ldapHelper.getUserinfo("thomasp");
        assertEquals("Name must be set", "Thomas", thomaspUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Pringle", thomaspUserIdentity.getLastName());
        assertEquals("UserId must be set", "thomas.pringle@altran.com", thomaspUserIdentity.getUid());

        WhydahUserIdentity erikdUserIdentity = ldapHelper.getUserinfo("erikd");
        assertEquals("Name must be set", "Erik", erikdUserIdentity.getFirstName());
        assertEquals("Lastname must be set", "Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("UserId must be set", "erik.drolshammer", erikdUserIdentity.getUid());
        
        WhydahUser whydahUser1 = new WhydahUser(thomaspUserIdentity, roleRepository.getUserPropertyAndRoles(thomaspUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles = whydahUser1.getPropsAndRoles();
        assertEquals("All roles must be found", 3, propsAndRoles.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "thomas.pringle@altran.com", "2", "Mobilefirst", "5", "Altran", "developer", "30"));
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles, "thomas.pringle@altran.com", "3", "Whydah", "1", "Whydah", "developer", "20"));

        WhydahUser whydahUser2 = new WhydahUser(erikdUserIdentity, roleRepository.getUserPropertyAndRoles(erikdUserIdentity.getUid()));
        
        List<UserPropertyAndRole> propsAndRoles2 = whydahUser2.getPropsAndRoles();
        assertEquals("All roles must be found", 1, propsAndRoles2.size());
        assertTrue("The role must be found", containsRoleMapping(propsAndRoles2, "erik.drolshammer", "2", "Mobilefirst", "5", "Altran", "admin", "70"));

    }

	private boolean containsRoleMapping(List<UserPropertyAndRole> propsAndRoles, String uid,  String appId, String appName, String orgId, String orgName, String roleName, String roleId) {
		for (UserPropertyAndRole role : propsAndRoles) {
			if(role.getAppId().equals(appId) && 
			   role.getApplicationName().equals(appName) && 
			   role.getOrganizationName().equals(orgName) && 
			   role.getOrgId().equals(orgId) &&
			   role.getRoleName().equals(roleName) && 
			   role.getRoleValue().equals(roleId) && 
			   role.getUid().equals(uid)) {
				
				return true;
			}
		}
		return false;
	}
}
