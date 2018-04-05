package net.whydah.identity.dataimport;

import com.jayway.restassured.RestAssured;
import net.whydah.identity.Main;
import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.junit.Ignore;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class IamDataImporterTest {
    private BasicDataSource dataSource;
    private IamDataImporter dataImporter;
    private Main main;
    String applicationsImportSource;

    @BeforeClass
    public void startServer() {
        ApplicationMode.setCIMode();
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("classpath:useridentitybackend-test.properties"))
                .done()
                .getConfiguration();


        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        applicationsImportSource = configuration.evaluateToString("import.applicationssource");
        FileUtils.deleteDirectory(roleDBDirectory);
        dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6655);
        main.startEmbeddedDS(configuration.asMap());

        dataImporter = new IamDataImporter(dataSource, configuration);

        main.startJetty();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    private BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
        FileUtils.deleteDirectory(new File("target/data/"));
    }
    
    @Test
    public void testDataIsImported() throws Exception {
        dataImporter.importIamData();
        LdapUserIdentityDao ldapUserIdentityDao = dataImporter.getLdapUserIdentityDao();

        LDAPUserIdentity erikdUserIdentity = ldapUserIdentityDao.getUserIndentity("erikd");
        assertEquals("Erik", erikdUserIdentity.getFirstName());
        assertEquals("Drolshammer", erikdUserIdentity.getLastName());
        assertEquals("erik.drolshammer", erikdUserIdentity.getUid());

        ApplicationService applicationService = new ApplicationService(new ApplicationDao(dataSource), null, null, null);
        UserAggregateService userAggregateService = new UserAggregateService(null, dataImporter.getUserApplicationRoleEntryDao(),
                applicationService, null, null);


        UserAggregate userAggregate2 = UserAggregateMapper.fromUserIdentityJson(UserIdentityMapper.toJson(erikdUserIdentity));
        List<UserApplicationRoleEntry> userApplicationRoleEntries = userAggregateService.getUserApplicationRoleEntries(erikdUserIdentity.getUid());
        userAggregate2.setRoleList(userApplicationRoleEntries);

        List<UserApplicationRoleEntry> propsAndRoles2 = userAggregate2.getRoleList();
        assertEquals(1, propsAndRoles2.size());
        //assertTrue(containsRoleMapping(propsAndRoles2, "erik.drolshammer", "2212", "Whydah-UserAdminService", "Capra Consulting", "WhydahUserAdmin", "70"));
    }

    private boolean containsRoleMapping(List<UserApplicationRoleEntry> propsAndRoles, String uid, String appId, String appName, String orgName, String roleName, String roleValue) {
        for (UserApplicationRoleEntry role : propsAndRoles) {
            if (role.getApplicationId().equals(appId) &&
			   role.getApplicationName().equals(appName) &&
                    role.getOrgName().equals(orgName) &&
                    role.getRoleName().equals(roleName) &&
                    role.getRoleValue().equals(roleValue) &&
                    role.getUserId().equals(uid)) {
                return true;
			}
		}
		return false;
	}
}
