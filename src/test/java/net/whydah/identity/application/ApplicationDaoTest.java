package net.whydah.identity.application;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-18
 */
public class ApplicationDaoTest {
    private final static String basepath = "target/ApplicationDaoTest/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private static BasicDataSource dataSource;
    private static DatabaseMigrationHelper dbHelper;
    private static ApplicationDao applicationDao;

    @BeforeClass
    public static void init() throws Exception {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);

        dbHelper = new DatabaseMigrationHelper(dataSource);
        applicationDao = new ApplicationDao(dataSource);
    }

    @Before
    public void cleanDB() {
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();
        assertEquals(applicationDao.countApplications(), 0);
    }


    @Test
    public void testAddAndGet() {
        Application application = new Application("appId1", "appName1");
        application.setSecret("verySecretKeyHere");
        application.setDefaultRoleName("defaultRoleName1");
        application.setDefaultOrgName("defaultOrgName1");
        application.setDescription("description1");
        application.addRole(new Role("roleId1", "roleName1"));
        application.addRole(new Role("roleId2", "roleName2"));
        application.setAvailableOrgNames(Arrays.asList("orgName1", "orgName2", "orgName3"));

        Application fromDb = applicationDao.create(application);

        assertNotNull(fromDb.getId());

        assertEquals(application.getId(), fromDb.getId());
        assertEquals(application.getName(), fromDb.getName());
        assertEquals(application.getDefaultRoleName(), fromDb.getDefaultRoleName());
        assertEquals(application.getDefaultOrgName(), fromDb.getDefaultOrgName());
        assertEquals(fromDb.getAvailableRoles().size(), 2);
        assertEquals(fromDb.getAvailableOrgNames().size(), 3);
        assertEquals(fromDb.getSecret(), application.getSecret());
    }
}
