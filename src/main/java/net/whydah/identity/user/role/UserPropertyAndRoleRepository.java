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

public class UserPropertyAndRoleRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserPropertyAndRoleRepository.class);

    private static final String GET_USERROLES_SQL = "SELECT UserID, AppID, OrganizationId, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
    private static final String INSERT_USERROLE_SQL = "INSERT INTO UserRoles (UserID, AppID, OrganizationId, RoleName, RoleValues) values (?, ?, ?, ?, ?)";
    private static final String DELETE_USER_SQL = "DELETE FROM UserRoles WHERE UserID=?";
    private static final String DELETE_ROLE_SQL = "DELETE FROM UserRoles WHERE UserID=? AND AppID=? AND OrganizationId=? AND RoleName=?";
    private static final String DELETE_APP_ROLES_SQL = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
    private static final String UPDATE_SQL = "UPDATE UserRoles set RoleValues=? WHERE UserID=? AND AppID=? AND OrganizationId=? AND RoleName=?";

    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private QueryRunner queryRunner;

    public UserPropertyAndRoleRepository() {
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        logger.debug("Searching for roles for {}", uid);

        List<UserPropertyAndRole> roles;
        try {
            roles = queryRunner.query(GET_USERROLES_SQL, new UserRolesResultsetHandler(), uid);
        } catch (SQLException e) {
            throw new DatastoreException("Error fetching roles for user with uid=" + uid, e);
        }
        logger.debug("Found {} roles", roles != null ? roles.size() : "null");
        
       /* TODO Just for tests
        for(UserPropertyAndRole obj : resultat){
        	logger.info("UID: "+obj.getUid());
        	logger.info("Role name: "+obj.getRoleName());
        }
        */
       
        return roles;
    }

    public boolean hasRole(String uid, UserPropertyAndRole role) {
        List<UserPropertyAndRole> existingRoles = getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            boolean roleExist = existingRole.getApplicationId().equals(role.getApplicationId())
                    && existingRole.getOrganizationId().equals(role.getOrganizationId())
                    && existingRole.getApplicationRoleName().equals(role.getApplicationRoleName());
            if (roleExist) {
                return true;
            }
        }
        return false;
    }


    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        try {
            queryRunner.update(INSERT_USERROLE_SQL,
                    userPropertyAndRole.getUid(),
                    userPropertyAndRole.getApplicationId(),
                    userPropertyAndRole.getOrganizationId(),
                    userPropertyAndRole.getApplicationRoleName(),
                    userPropertyAndRole.getApplicationRoleValue()
            );
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

    public void deleteUserRole(String uid, String appid, String orgid, String rolename) {
        try {
            queryRunner.update(DELETE_ROLE_SQL, uid, appid, orgid, rolename);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public void deleteUserAppRoles(String uid, String appid) {
        try {
            queryRunner.update(DELETE_APP_ROLES_SQL, uid, appid);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public void updateUserRoleValue(String uid, String appid, String orgid, String rolename, String rolevalue) {
        try {
            queryRunner.update(UPDATE_SQL, rolevalue, uid, appid, orgid, rolename);
        } catch (SQLException e) {
            throw new DatastoreException(e);
        }
    }

    public String getOrgname(String orgid) {
        try {
            String ORG_SQL = "SELECT Name from Organization WHERE id=?";
            return queryRunner.query(ORG_SQL, new OrgnameResultSetHandler(), orgid);
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
                userPropertyAndRole.setUid(rs.getString(1));
                userPropertyAndRole.setApplicationId(rs.getString(2));
                userPropertyAndRole.setOrganizationId(rs.getString(3));
                userPropertyAndRole.setApplicationRoleName(rs.getString(4));
                userPropertyAndRole.setApplicationRoleValue(null2empty(rs.getString(5)));
                Application application = applicationRepository.getApplication(userPropertyAndRole.getApplicationId());
                if(application != null) {
                    userPropertyAndRole.setApplicationName(application.getName());
                }
                String orgName = getOrgname(userPropertyAndRole.getOrganizationId());
                if(orgName != null) {
                    userPropertyAndRole.setOrganizationName(orgName);
                }
                result.add(userPropertyAndRole);
            }
            return result;
        }

        private String null2empty(String in) {
            return in != null ? in : "";
        }
    }


}
