package net.whydah.identity.user.role;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-18
 */
public class UserPropertyAndRoleDaoTest {
    private final static String basepath = "target/UserPropertyAndRoleDaoTest/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private static BasicDataSource dataSource;
    private static DatabaseMigrationHelper dbHelper;
    private static UserApplicationRoleEntryDao roleRepository;

    @BeforeClass
    public static void init() throws Exception {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);

        dbHelper = new DatabaseMigrationHelper(dataSource);
        roleRepository = new UserApplicationRoleEntryDao(dataSource);
    }

    @Before
    public void cleanDB() {
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();
        assertEquals(roleRepository.countUserRolesInDB(), 0);
    }
    
    @AfterClass
    public static void close() {
    	try {
        	if(!dataSource.isClosed()) {
        		dataSource.close();
        	}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    @Test
    public void testAddAndGet() {
        Map<String, UserApplicationRoleEntry> added = addTestRoles("uid1", 1);
        for (UserApplicationRoleEntry expected : added.values()) {
            UserApplicationRoleEntry fromDb = roleRepository.getUserApplicationRoleEntry(expected.getId());
            roleEquals(expected, fromDb);
        }
    }

    @Test
    public void testGetUserPropertyAndRoles() {
        String uid = "uid2";
        Map<String, UserApplicationRoleEntry> added = addTestRoles(uid, 3);
        List<UserApplicationRoleEntry> roles = roleRepository.getUserApplicationRoleEntries(uid);
        assertEquals(added.size(), roles.size());
        for (UserApplicationRoleEntry fromDb : roles) {
            UserApplicationRoleEntry expected = added.get(fromDb.getId());
            roleEquals(expected, fromDb);
        }
    }

    @Test
    public void testCountUserRolesInDB() {
        String uid3 = "uid3";
        Map<String, UserApplicationRoleEntry> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserApplicationRoleEntry> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());
    }

    /*
    @Test
    public void testDeleteRolesForUid() {
        String uid3 = "uid3";
        Map<String, UserPropertyAndRole> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserPropertyAndRole> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        roleRepository.deleteUser(uid4);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size());
        assertEquals(roleRepository.getUserPropertyAndRoles(uid4).size(), 0);
    }
    */

    @Test
    public void testDeleteUserAppRoles() {
        String uid3 = "uid3";
        Map<String, UserApplicationRoleEntry> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserApplicationRoleEntry> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        String appIdToDelete = "appId2";
        roleRepository.deleteUserAppRoles(uid4, appIdToDelete);
        int expectedRolesForUid4 = added4.size() - 1;
        assertEquals(roleRepository.countUserRolesInDB(), (added3.size() + expectedRolesForUid4));
        assertEquals(roleRepository.getUserApplicationRoleEntries(uid4).size(), expectedRolesForUid4);
    }

    @Test
    public void testDeleteRoleByRoleId() {
        String uid3 = "uid3";
        Map<String, UserApplicationRoleEntry> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserApplicationRoleEntry> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        roleRepository.deleteRoleByRoleID("roleId1");   //Expect role to be remove for both users
        assertEquals(roleRepository.countUserRolesInDB(), (added3.size() - 1 + added4.size() - 1));
        assertEquals(roleRepository.getUserApplicationRoleEntries(uid3).size(), 1);
        assertEquals(roleRepository.getUserApplicationRoleEntries(uid4).size(), 2);
    }


    private Map<String, UserApplicationRoleEntry> addTestRoles(String uid, int count) {
        Map<String, UserApplicationRoleEntry> added = new HashMap<>();
        UserApplicationRoleEntry role;
        for (int i = 1; i <= count; i++) {
            role = new UserApplicationRoleEntry();
            role.setId("roleId" + i);
            role.setUserId(uid);
            role.setApplicationId("appId" + i);
            role.setRoleName("appRoleName" + i);
            role.setRoleValue("appRoleValue" + i);
            //private transient String applicationName;
            //private transient String organizationName;
            added.put(role.getId(), role);
            roleRepository.addUserApplicationRoleEntry(role);
        }
        return added;
    }

    private void roleEquals(UserApplicationRoleEntry role, UserApplicationRoleEntry fromDb) {
        //assertTrue(fromDb.equals(role));  //Not possible because of null to "" in getters.

        assertEquals(fromDb.getId(), role.getId());
        assertEquals(fromDb.getUserId(), role.getUserId());
        assertEquals(fromDb.getApplicationId(), role.getApplicationId());
        assertEquals(fromDb.getRoleName(), role.getRoleName());
        assertEquals(fromDb.getRoleValue(), role.getRoleValue());
        assertEquals(fromDb.getApplicationName(), role.getApplicationName());
        assertEquals(fromDb.getOrgName(), role.getOrgName());
    }
}
