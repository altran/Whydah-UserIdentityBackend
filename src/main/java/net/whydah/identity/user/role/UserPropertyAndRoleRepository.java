package net.whydah.identity.user.role;

import com.google.inject.Inject;
import net.whydah.identity.application.Application;
import net.whydah.identity.application.ApplicationRepository;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//ED thinks org.apache.commons.dbutils is crappy. Please consider using some other library.
public class UserPropertyAndRoleRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserPropertyAndRoleRepository.class);

    private static final String GET_ALL_USER_ROLES = "SELECT * FROM UserRoles";
    private static final String GET_USERROLES_SQL = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
    private static final String GET_USERROLE_SQL = "SELECT RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues FROM UserRoles WHERE RoleID=?";
    private static final String INSERT_USERROLE_SQL = "INSERT INTO UserRoles (RoleID, UserID, AppID, OrganizationName, RoleName, RoleValues) values (?, ?, ?, ?, ?, ?)";
    private static final String DELETE_USER_SQL = "DELETE FROM UserRoles WHERE UserID=?";
    private static final String DELETE_ROLE_SQL = "DELETE FROM UserRoles WHERE RoleID=?";
    private static final String DELETE_APP_ROLES_SQL = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
    private static final String UPDATE_SQL = "UPDATE UserRoles set RoleValues=? WHERE RoleID=?";

    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private QueryRunner queryRunner;

    public UserPropertyAndRoleRepository() {
    }

    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        logger.debug("Searching for role for roleId {}", roleId);

        UserPropertyAndRole role;
        try {
            role = queryRunner.query(GET_USERROLE_SQL, new UserRoleResultsetHandler(), roleId);
        } catch (SQLException e) {
            throw new DatastoreException("Error fetching roles for user with roleId=" + roleId, e);
        }

        return role;
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        logger.debug("getUserPropertyAndRoles for uid {}", uid);

        List<UserPropertyAndRole> roles = new ArrayList<>();
        if (uid != null) {
            try {
                roles = queryRunner.query(GET_USERROLES_SQL, new UserRolesResultsetHandler(), uid);
            } catch (SQLException e) {
                logger.warn("getUserPropertyAndRoles failed for uid={}. SQLException: {}", uid, e.getMessage());
                //throw new DatastoreException("Error fetching roles for user with uid=" + uid, e);
            }
        }
        logger.debug("Found {} roles for uid={}", (roles != null ? roles.size() : "null"), uid);
        return roles;
    }

    public int countUserRolesInDB() throws SQLException{
        logger.debug("Counting user roles in DB");
        return queryRunner.query(GET_ALL_USER_ROLES, new UserRolesResultsetHandler()).size();
    }

    public boolean hasRole(String uid, UserPropertyAndRole role) {
        List<UserPropertyAndRole> existingRoles = getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrganizationName().equals(role.getOrganizationName())
                    && existingRole.getApplicationRoleName().equals(role.getApplicationRoleName());
            if (roleExist) {
                return true;
            }
        }
        return false;
    }


    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        if (userPropertyAndRole.getRoleId() == null) {
            userPropertyAndRole.setRoleId(UUID.randomUUID().toString());
        }
        try {
            queryRunner.update(INSERT_USERROLE_SQL,
                    userPropertyAndRole.getRoleId(),
                    userPropertyAndRole.getUid(),
                    userPropertyAndRole.getApplicationId(),
                    userPropertyAndRole.getOrganizationName(),
                    userPropertyAndRole.getApplicationRoleName(),
                    userPropertyAndRole.getApplicationRoleValue()

            );
            logger.trace(INSERT_USERROLE_SQL+":"+userPropertyAndRole);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    /**
     * Removes any roles for a user.
     * @param uid userid
     */
    public void deleteUser(String uid) {
        try {
            queryRunner.update(DELETE_USER_SQL, uid);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public void deleteUserRole(String uid, String roleId) {
        deleteRole(roleId);
    }

    public void deleteUserAppRoles(String uid, String appid) {
        try {
            queryRunner.update(DELETE_APP_ROLES_SQL, uid, appid);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public void updateUserRoleValue(UserPropertyAndRole role) {
        updateUserRoleValue(role.getUid(), role.getRoleId(), role.getApplicationRoleValue());
    }

    public void updateUserRoleValue(String uid, String roleId, String rolevalue) {
        try {
            queryRunner.update(UPDATE_SQL, rolevalue, roleId);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public String getOrgname(String orgid) {
        try {
            String ORG_SQL = "SELECT Name from Organization WHERE AppID=?";
            return queryRunner.query(ORG_SQL, new OrgnameResultSetHandler(), orgid);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public void deleteRole(String roleId) {
        try {
            queryRunner.update(DELETE_ROLE_SQL, roleId);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    private static class OrgnameResultSetHandler implements ResultSetHandler<String> {
        @Override
        public String handle(ResultSet rs) throws SQLException {
            if(rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        }
    }



    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void setApplicationRepository(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    private class UserRolesResultsetHandler implements ResultSetHandler<List<UserPropertyAndRole>> {
        @Override
        public List<UserPropertyAndRole> handle(ResultSet rs) throws SQLException {
            ArrayList<UserPropertyAndRole> result = new ArrayList<>();
            while(rs.next()) {
                UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
                userPropertyAndRole.setRoleId(rs.getString(1));
                userPropertyAndRole.setUid(rs.getString(2));
                userPropertyAndRole.setApplicationId(rs.getString(3));
                userPropertyAndRole.setOrganizationName(rs.getString(4));
                userPropertyAndRole.setApplicationRoleName(rs.getString(5));
                userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString(6)));
                Application application = applicationRepository.getApplication(userPropertyAndRole.getApplicationId());
                if(application != null) {
                    userPropertyAndRole.setApplicationName(application.getName());
                }
                result.add(userPropertyAndRole);
            }
            return result;
        }

        private String null2empty(String in) {
            return in != null ? in : "";
        }
    }

    private class UserRoleResultsetHandler implements ResultSetHandler<UserPropertyAndRole> {
        @Override
        public UserPropertyAndRole handle(ResultSet rs) throws SQLException {
            while(rs.next()) {
                UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
                userPropertyAndRole.setRoleId(rs.getString(1));
                userPropertyAndRole.setUid(rs.getString(2));
                userPropertyAndRole.setApplicationId(rs.getString(3));
                userPropertyAndRole.setOrganizationName(rs.getString(4));
                userPropertyAndRole.setApplicationRoleName(rs.getString(5));
                userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString(6)));
                Application application = applicationRepository.getApplication(userPropertyAndRole.getApplicationId());
                if(application != null) {
                    userPropertyAndRole.setApplicationName(application.getName());
                }
                return userPropertyAndRole;
            }
            return null;
        }

        private String null2empty(String in) {
            return in != null ? in : "";
        }
    }

}
