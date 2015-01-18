package net.whydah.identity.user.role;

import com.google.inject.Inject;
import net.whydah.identity.application.Application;
import net.whydah.identity.application.ApplicationRepository;

import java.sql.SQLException;
import java.util.List;

public class UserPropertyAndRoleRepository {
    private final UserPropertyAndRoleDao userPropertyAndRoleDao;
    private final ApplicationRepository applicationRepository;

    @Inject
    public UserPropertyAndRoleRepository(UserPropertyAndRoleDao userPropertyAndRoleDao, ApplicationRepository applicationRepository) {
        this.userPropertyAndRoleDao = userPropertyAndRoleDao;
        this.applicationRepository = applicationRepository;
    }

    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        UserPropertyAndRole role = userPropertyAndRoleDao.getUserPropertyAndRole(roleId);
        Application application = applicationRepository.getApplication(role.getApplicationId());
        if (application != null) {
            role.setApplicationName(application.getName());
        }
        return role;
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        List<UserPropertyAndRole> roles = userPropertyAndRoleDao.getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole role : roles) {
            Application application = applicationRepository.getApplication(role.getApplicationId());
            if (application != null) {
                role.setApplicationName(application.getName());
            }
        }
        return roles;
    }

    public int countUserRolesInDB() throws SQLException{
        return userPropertyAndRoleDao.countUserRolesInDB();
    }

    public boolean hasRole(String uid, UserPropertyAndRole role) {
        return userPropertyAndRoleDao.hasRole(uid, role);
    }


    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        userPropertyAndRoleDao.addUserPropertyAndRole(userPropertyAndRole);
    }


    public void deleteUser(String uid) {
        userPropertyAndRoleDao.deleteUser(uid);
    }

    public void deleteUserRole(String uid, String roleId) {
        userPropertyAndRoleDao.deleteUserRole(uid, roleId);
    }

    public void deleteUserAppRoles(String uid, String appid) {
        userPropertyAndRoleDao.deleteUserAppRoles(uid, appid);
    }

    public void updateUserRoleValue(UserPropertyAndRole role) {
        userPropertyAndRoleDao.updateUserRoleValue(role.getUid(), role.getRoleId(), role.getApplicationRoleValue());
    }

    public void updateUserRoleValue(String uid, String roleId, String rolevalue) {
        userPropertyAndRoleDao.updateUserRoleValue(uid, roleId, rolevalue);
    }

    public String getOrgname(String orgid) {
        return userPropertyAndRoleDao.getOrgname(orgid);
    }

    public void deleteRole(String roleId) {
        userPropertyAndRoleDao.deleteRole(roleId);
    }
}
