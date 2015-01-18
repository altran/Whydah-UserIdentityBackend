package net.whydah.identity.user.role;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
public class UserPropertyAndRoleDao {
    private static final Logger logger = LoggerFactory.getLogger(UserPropertyAndRoleRepository.class);
    private JdbcTemplate jdbcTemplate;

    @Inject
    public UserPropertyAndRoleDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        logger.debug("getUserPropertyAndRole for roleId {}", roleId);
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE RoleID=?";
        return this.jdbcTemplate.queryForObject(sql, new String[]{roleId}, new UserPropertyAndRoleMapper());
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        logger.debug("getUserPropertyAndRoles for uid={}", uid);
        String sql = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
        List<UserPropertyAndRole> roles = this.jdbcTemplate.query(sql, new String[]{uid}, new UserPropertyAndRoleMapper());
        logger.debug("Found {} roles for uid={}", (roles != null ? roles.size() : "null"), uid);
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

            //TODO
                /*
                Application application = applicationRepository.getApplication(userPropertyAndRole.getApplicationId());
                if (application != null) {
                    userPropertyAndRole.setApplicationName(application.getName());
                }
                */
            return userPropertyAndRole;
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
        logger.debug("countUserRolesInDB={}", count);
        return count;
    }

    //TODO Can it be private/protected?
    public boolean hasRole(String uid, UserPropertyAndRole role) {
        List<UserPropertyAndRole> existingRoles = getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            logger.trace("hasRole - checking existing.applicationID {} against applicationID {}", existingRole.getApplicationId(), role.getApplicationId());
            logger.trace("hasRole - checking existing.getOrganizationName {} against getOrganizationName {}", existingRole.getOrganizationName(), role.getOrganizationName());
            logger.trace("hasRole - checking existing.getApplicationRoleName {} against getApplicationRoleName {}", existingRole.getApplicationRoleName(), role.getApplicationRoleName());
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrganizationName().equals(role.getOrganizationName())
                    && existingRole.getApplicationRoleName().equals(role.getApplicationRoleName());
            if (roleExist) {
                logger.trace("Found role");
                return true;
            }
        }
        logger.trace("Not Found role");
        return false;
    }


    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        logger.trace("addUserPropertyAndRole:" + userPropertyAndRole);
        if (hasRole(userPropertyAndRole.getUid(), userPropertyAndRole)) {
            logger.trace("Trying to add an existing role, ignoring");
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
        logger.trace(rows + " roles added");
        logger.trace(sql +":" + userPropertyAndRole);
    }

    /**
     * Removes any roles for a user.
     * @param uid userid
     */
    public void deleteUser(String uid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=?";
        jdbcTemplate.update(sql, uid);
    }


    //TODO This looks like a bug!
    public void deleteUserRole(String uid, String roleId) {
        deleteRole(roleId);
    }

    public void deleteUserAppRoles(String uid, String appid) {
        String sql = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
        jdbcTemplate.update(sql, uid, appid);
    }

    public void updateUserRoleValue(UserPropertyAndRole role) {
        updateUserRoleValue(role.getUid(), role.getRoleId(), role.getApplicationRoleValue());
    }
    //TODO What about uid?
    public void updateUserRoleValue(String uid, String roleId, String rolevalue) {
        String sql = "UPDATE UserRoles set RoleValues=? WHERE RoleID=?";
        jdbcTemplate.update(sql, rolevalue, roleId);
    }


    //TODO Bug? orgid vs AppID?
    public String getOrgname(String orgid) {
        String sql = "SELECT Name from Organization WHERE AppID=?";
        return jdbcTemplate.queryForObject(sql, new String[]{orgid}, String.class);
    }

    public void deleteRole(String roleId) {
        String sql = "DELETE FROM UserRoles WHERE RoleID=?";
        jdbcTemplate.update(sql, roleId);
    }
}

