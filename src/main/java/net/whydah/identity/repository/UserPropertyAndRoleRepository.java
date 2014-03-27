package net.whydah.identity.repository;

import com.google.inject.Inject;
import net.whydah.identity.domain.Application;
import net.whydah.identity.domain.UserPropertyAndRole;
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
    @Inject private BackendConfigDataRepository backendConfigDataRepository;

    private static final String GET_USERROLES_SQL = "SELECT UserID, AppID, OrganizationId, RoleName, RoleValues FROM UserRoles WHERE UserID=?";
    private static final String INSERT_USERROLE_SQL = "INSERT INTO UserRoles (UserID, AppID, OrganizationId, RoleName, RoleValues) values (?, ?, ?, ?, ?)";
    private static final String DELETE_USER_SQL = "DELETE FROM UserRoles WHERE UserID=?";
    private static final String DELETE_ROLE_SQL = "DELETE FROM UserRoles WHERE UserID=? AND AppID=? AND OrganizationId=? AND RoleName=?";
    private static final String DELETE_APP_ROLES_SQL = "DELETE FROM UserRoles WHERE UserID=? AND AppID=?";
    private static final String UPDATE_SQL = "UPDATE UserRoles set RoleValues=? WHERE UserID=? AND AppID=? AND OrganizationId=? AND RoleName=?";

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
            boolean roleExist = existingRole.getAppId().equals(role.getAppId())
                    && existingRole.getOrgId().equals(role.getOrgId())
                    && existingRole.getRoleName().equals(role.getRoleName());
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
                    userPropertyAndRole.getAppId(),
                    userPropertyAndRole.getOrgId(),
                    userPropertyAndRole.getRoleName(),
                    userPropertyAndRole.getRoleValue()
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

    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void setBackendConfigDataRepository(BackendConfigDataRepository backendConfigDataRepository) {
        this.backendConfigDataRepository = backendConfigDataRepository;
    }

    private class UserRolesResultsetHandler implements ResultSetHandler<List<UserPropertyAndRole>> {
        @Override
        public List<UserPropertyAndRole> handle(ResultSet rs) throws SQLException {
            ArrayList<UserPropertyAndRole> result = new ArrayList<UserPropertyAndRole>();
            while(rs.next()) {
                UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
                userPropertyAndRole.setUid(rs.getString(1));
                userPropertyAndRole.setAppId(rs.getString(2));
                userPropertyAndRole.setOrgId(rs.getString(3));
                userPropertyAndRole.setRoleName(rs.getString(4));
                userPropertyAndRole.setRoleValue(null2empty(rs.getString(5)));
                Application application = backendConfigDataRepository.getApplication(userPropertyAndRole.getAppId());
                if(application != null) {
                    userPropertyAndRole.setApplicationName(application.getName());
                }
                String orgName = backendConfigDataRepository.getOrgname(userPropertyAndRole.getOrgId());
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
