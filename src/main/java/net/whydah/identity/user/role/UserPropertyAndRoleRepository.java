package net.whydah.identity.user.role;

import com.google.inject.Inject;
import net.whydah.identity.application.Application;
import net.whydah.identity.application.ApplicationDao;

import java.util.List;

public class UserPropertyAndRoleRepository {
    private final UserPropertyAndRoleDao userPropertyAndRoleDao;
    private final ApplicationDao applicationDao;

    @Inject
    public UserPropertyAndRoleRepository(UserPropertyAndRoleDao userPropertyAndRoleDao, ApplicationDao applicationDao) {
        this.userPropertyAndRoleDao = userPropertyAndRoleDao;
        this.applicationDao = applicationDao;
    }

    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        UserPropertyAndRole role = userPropertyAndRoleDao.getUserPropertyAndRole(roleId);
        Application application = applicationDao.getApplication(role.getApplicationId());
        if (application != null) {
            role.setApplicationName(application.getName());
        }
        return role;
    }

    public List<UserPropertyAndRole> getUserPropertyAndRoles(String uid) {
        List<UserPropertyAndRole> roles = userPropertyAndRoleDao.getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole role : roles) {
            Application application = applicationDao.getApplication(role.getApplicationId());
            if (application != null) {
                role.setApplicationName(application.getName());
            }
        }
        return roles;
    }

    public int countUserRolesInDB() {
        return userPropertyAndRoleDao.countUserRolesInDB();
    }

    public boolean hasRole(String uid, UserPropertyAndRole role) {
        return userPropertyAndRoleDao.hasRole(uid, role);
    }


    public void addUserPropertyAndRole(final UserPropertyAndRole userPropertyAndRole) {
        userPropertyAndRoleDao.addUserPropertyAndRole(userPropertyAndRole);
    }


    public void deleteUser(String uid) {
        userPropertyAndRoleDao.deleteAllRolesForUser(uid);
    }

    public void deleteUserRole(String uid, String roleId) {
        userPropertyAndRoleDao.deleteUserRole(uid, roleId);
    }

    public void updateUserRoleValue(UserPropertyAndRole role) {
        userPropertyAndRoleDao.updateUserRoleValue(role);
    }

    public void deleteRole(String roleId) {
        userPropertyAndRoleDao.deleteRole(roleId);
    }
}
