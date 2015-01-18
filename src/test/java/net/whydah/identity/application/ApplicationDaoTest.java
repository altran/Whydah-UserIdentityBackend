package net.whydah.identity.application;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Application application = new Application("appId1", "appName1", "defaultRoleName1", "defaultOrgName1");
        Application fromDb = applicationDao.create(application);

        assertNotNull(fromDb.getId());

        assertEquals(application.getId(), fromDb.getId());
        assertEquals(application.getName(), fromDb.getName());
        assertEquals(application.getDefaultRoleName(), fromDb.getDefaultRoleName());
        assertEquals(application.getDefaultOrgName(), fromDb.getDefaultOrgName());
    }
}
