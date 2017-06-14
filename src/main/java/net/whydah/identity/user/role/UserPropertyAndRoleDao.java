package net.whydah.identity.user.role;

import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Responsible for fetching any user roles or properties stored in RDBMS.
 * Backed by spring-jdbc, http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-18
 */
@Repository
public class UserPropertyAndRoleDao {
    private static final Logger log = LoggerFactory.getLogger(UserPropertyAndRoleDao.class);
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public UserPropertyAndRoleDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        log.debug("getUserPropertyAndRole for roleId {}", roleId);
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE RoleID=?";
        List<UserPropertyAndRole> roles = jdbcTemplate.query(sql, new String[]{roleId}, new UserPropertyAndRoleMapper());
        if (roles.isEmpty()) {
            return null;
        }
        return roles.get(0);
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
        List<UserPropertyAndRole> roles = this.jdbcTemplate.query(sql, new String[]{uid}, new UserPropertyAndRoleMapper());
        log.debug("Found {} roles for uid={}", (roles != null ? roles.size() : "null"), uid);
        return roles;
    }

    private static final class UserPropertyAndRoleMapper implements RowMapper<UserPropertyAndRole> {
        public UserPropertyAndRole mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
            userPropertyAndRole.setRoleId(rs.getString("RoleID").trim());
            userPropertyAndRole.setUid(rs.getString("UserID").trim());
            userPropertyAndRole.setApplicationId(rs.getString("AppID"));
            userPropertyAndRole.setOrganizationName(rs.getString("OrganizationName"));
            userPropertyAndRole.setApplicationRoleName(rs.getString("RoleName"));
            //userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString("RoleValues")));
            userPropertyAndRole.setApplicationRoleValue(rs.getString("RoleValues"));

            return userPropertyAndRole;
        }
        /*
        private String null2empty(String in) {
            return in != null ? in : "";
        }
        */
    }

    private static final class UserApplicationRoleEntryMapper implements RowMapper<UserApplicationRoleEntry> {
        public UserApplicationRoleEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserApplicationRoleEntry userApplicationRoleEntry = new UserApplicationRoleEntry();
            userApplicationRoleEntry.setId(rs.getString("RoleID").trim());
            userApplicationRoleEntry.setUserId(rs.getString("UserID").trim());
            userApplicationRoleEntry.setApplicationId(rs.getString("AppID"));
            userApplicationRoleEntry.setOrgName(rs.getString("OrganizationName"));
            userApplicationRoleEntry.setRoleName(rs.getString("RoleName"));
            //userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString("RoleValues")));
            userApplicationRoleEntry.setRoleValue(rs.getString("RoleValues"));

            return userApplicationRoleEntry;
        }
        /*
        private String null2empty(String in) {
            return in != null ? in : "";
        }
        */
    }

    public int countUserRolesInDB() {
        String sql = "SELECT count(*) FROM UserRoles";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        log.debug("countUserRolesInDB={}", count);
        return count;
    }

    public boolean hasRole(String uid, UserPropertyAndRole role) {
        List<UserPropertyAndRole> existingRoles = getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            log.trace("hasRole - checking existing.applicationID {} against applicationID {} " +
                    "\n & existing.getOrganizationName {} against getOrganizationName {}" +
                    "\n & existing.getApplicationRoleName {} against getApplicationRoleName {}",
                    existingRole.getApplicationId(), role.getApplicationId(),
                    existingRole.getOrganizationName(), role.getOrganizationName(),
                    existingRole.getApplicationRoleName(), role.getApplicationRoleName());
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrganizationName().equals(role.getOrganizationName())
                    && existingRole.getApplicationRoleName().equals(role.getApplicationRoleName());
            if (roleExist) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String uid, UserApplicationRoleEntry role) {
        List<UserPropertyAndRole> existingRoles = getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            log.trace("hasRole - checking existing.applicationID {} against applicationID {} " +
                            "\n & existing.getOrganizationName {} against getOrganizationName {}" +
                            "\n & existing.getApplicationRoleName {} against getApplicationRoleName {}",
                    existingRole.getApplicationId(), role.getApplicationId(),
                    existingRole.getOrganizationName(), role.getOrgName(),
                    existingRole.getApplicationRoleName(), role.getApplicationName());
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrganizationName().equals(role.getOrgName())
                    && existingRole.getApplicationRoleName().equals(role.getRoleName());
            if (roleExist) {
                return true;
            }
        }
        return false;
    }



    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        log.trace("addUserPropertyAndRole:" + userPropertyAndRole);
        if (hasRole(userPropertyAndRole.getUid(), userPropertyAndRole)) {
            log.trace("Trying to add an existing role, ignoring");
            return;
        }

        if (userPropertyAndRole.getRoleId() == null || userPropertyAndRole.getRoleId().length() < 5 ) {
            userPropertyAndRole.setRoleId(UUID.randomUUID().toString());
        }

        String sql = "INSERT INTO UserRoles (RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues) values (?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                userPropertyAndRole.getRoleId(),
                userPropertyAndRole.getUid(),
                userPropertyAndRole.getApplicationId(),
                userPropertyAndRole.getOrganizationName(),
                userPropertyAndRole.getApplicationRoleName(),
                userPropertyAndRole.getApplicationRoleValue()

        );
        log.trace("{} roles added, sql: {}", rows, userPropertyAndRole);
    }

    public void addUserPropertyAndRole(final UserApplicationRoleEntry userPropertyAndRole) {
        log.trace("addUserPropertyAndRole:" + userPropertyAndRole);
        if (hasRole(userPropertyAndRole.getUserId(), userPropertyAndRole)) {
            log.trace("Trying to add an existing role, ignoring");
            return;
        }

        if (userPropertyAndRole.getId() == null || userPropertyAndRole.getId().length() < 5 ) {
            userPropertyAndRole.setId(UUID.randomUUID().toString());
        }

        String sql = "INSERT INTO UserRoles (RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues) values (?, ?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                userPropertyAndRole.getId(),
                userPropertyAndRole.getUserId(),
                userPropertyAndRole.getApplicationId(),
                userPropertyAndRole.getOrgName(),
                userPropertyAndRole.getRoleName(),
                userPropertyAndRole.getRoleValue()

        );
        log.trace("{} roles added, sql: {}", rows, userPropertyAndRole);
    }

    public void deleteAllRolesForUser(String uid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=?";
        jdbcTemplate.update(sql, uid);
    }

    //TODO Is roleId globally unique?
    public void deleteUserRole(String uid, String roleId) {
        deleteRole(roleId);
    }
    public void deleteRole(String roleId) {
        String sql = "DELETE FROM UserRoles WHERE RoleID=?";
        jdbcTemplate.update(sql, roleId);
    }

    public void deleteUserAppRoles(String uid, String appid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
        jdbcTemplate.update(sql, uid, appid);
    }


    public void updateUserRoleValue(UserPropertyAndRole role) {
        String sql = "UPDATE UserRoles set RoleValues=? WHERE RoleID=? and UserID=?";
        jdbcTemplate.update(sql, role.getApplicationRoleValue(), role.getRoleId(), role.getUid());
    }
}

