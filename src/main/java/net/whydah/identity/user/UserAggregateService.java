package net.whydah.identity.user;

import net.whydah.identity.application.ApplicationService;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.resource.RoleRepresentationRequest;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleDao;
import net.whydah.identity.user.search.LuceneUserIndexer;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserIdentity;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Service
public class UserAggregateService {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final UserIdentityService userIdentityService;
    private final UserPropertyAndRoleDao userPropertyAndRoleDao;
    private final ApplicationService applicationDao;
    private final LuceneUserIndexer luceneIndexer;
    private final AuditLogDao auditLogDao;

    @Autowired
    public UserAggregateService(UserIdentityService userIdentityService, UserPropertyAndRoleDao userPropertyAndRoleDao,
                                ApplicationService applicationDao, LuceneUserIndexer luceneIndexer, AuditLogDao auditLogDao) {
        this.luceneIndexer = luceneIndexer;
        this.auditLogDao = auditLogDao;
        this.userPropertyAndRoleDao = userPropertyAndRoleDao;
        this.applicationDao = applicationDao;
        this.userIdentityService = userIdentityService;
    }


    @Deprecated
    public UIBUserAggregate getUIBUserAggregateByUsernameOrUid(String usernameOrUid) {
        LDAPUserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentity(usernameOrUid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with usernameOrUid=" + usernameOrUid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregateByUsernameOrUid could not find user with usernameOrUid={}", usernameOrUid);
            return null;
        }
        List<UserPropertyAndRole> userPropertyAndRoles = userPropertyAndRoleDao.getUserPropertyAndRoles(userIdentity.getUid());
        return new UIBUserAggregate(userIdentity, userPropertyAndRoles);
    }

    public UserAggregate getUserAggregateByUsernameOrUid(String usernameOrUid) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentity(usernameOrUid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with usernameOrUid=" + usernameOrUid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregateByUsernameOrUid could not find user with usernameOrUid={}", usernameOrUid);
            return null;
        }
        UserAggregate userAggregate = UserAggregateMapper.fromUserIdentityJson(UserIdentityMapper.toJson(userIdentity));
        List<UserApplicationRoleEntry> userApplicationRoleEntries = userPropertyAndRoleDao.getUserApplicationRoleEntries(userIdentity.getUid());
        userAggregate.setRoleList(userApplicationRoleEntries);
        return userAggregate;
    }

    @Deprecated
    public LDAPUserIdentity getUIBUserIdentityByUsernameOrUid(String usernameOrUid) {
        LDAPUserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentity(usernameOrUid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with usernameOrUid=" + usernameOrUid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregateByUsernameOrUid could not find user with usernameOrUid={}", usernameOrUid);
            return null;
        }
        return userIdentity;
    }

    public UserIdentity getUserIdentityByUsernameOrUid(String usernameOrUid) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentity(usernameOrUid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with usernameOrUid=" + usernameOrUid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregateByUsernameOrUid could not find user with usernameOrUid={}", usernameOrUid);
            return null;
        }
        return userIdentity;
    }

    @Deprecated
    public LDAPUserIdentity updateUserIdentity(String uid, LDAPUserIdentity newUserIdentity) {
        userIdentityService.updateUserIdentityForUid(uid, newUserIdentity);
        return newUserIdentity;
    }


    public void deleteUserAggregateByUid(String uid) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIdentityForUid(uid);
            luceneIndexer.removeFromIndex(uid);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIdentity with uid=" + uid, e);
        }
        if (userIdentity == null) {
            throw new IllegalArgumentException("LDAPUserIdentity not found. uid=" + uid);
        }
        try {
            userIdentityService.deleteUserIdentity(uid);
        } catch (NamingException ne) {
            log.warn("Trouble trying to delete user with uid:" + uid);
        }

        userPropertyAndRoleDao.deleteAllRolesForUser(uid);
        //indexer.removeFromIndex(uid);
        audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + userIdentity.getUsername());

    }


    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        String userId = null;
        if (authenticatedUser == null) {
//            log.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            userId = "-no-admin-user-";
        } else {
            userId = authenticatedUser.getUserName();
        }
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }

    private void audit(String uid, String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            log.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getUserName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(uid, now, action, what, value);
        auditLogDao.store(actionPerformed);
    }

    @Deprecated
    public UserPropertyAndRole addRole(String uid, RoleRepresentationRequest request) {
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(uid);
        role.setApplicationId(request.getApplicationId());
        role.setApplicationName(request.getApplicationName());
        role.setOrganizationName(request.getOrganizationName());
        role.setApplicationRoleName(request.getApplicationRoleName());
        role.setApplicationRoleValue(request.getApplicationRoleValue());

        return addRole(uid, role);
    }

    public UserApplicationRoleEntry addUserApplicationRoleEntry(String uid, RoleRepresentationRequest request) {
        UserApplicationRoleEntry role = new UserApplicationRoleEntry();
        role.setId(uid);
        role.setApplicationId(request.getApplicationId());
        role.setApplicationName(request.getApplicationName());
        role.setOrgName(request.getOrganizationName());
        role.setRoleName(request.getApplicationRoleName());
        role.setRoleValue(request.getApplicationRoleValue());

        return addRole(uid, role);
    }

    @Deprecated
    public UserPropertyAndRole addRoleIfNotExist(String uid, RoleRepresentationRequest request) {
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(uid);
        role.setApplicationId(request.getApplicationId());
        role.setApplicationName(request.getApplicationName());
        role.setOrganizationName(request.getOrganizationName());
        role.setApplicationRoleName(request.getApplicationRoleName());
        role.setApplicationRoleValue(request.getApplicationRoleValue());

        return addRoleIfNotExist(uid, role);
    }

    @Deprecated
    public List<UserPropertyAndRole> addRoles(String uid, List<UserPropertyAndRole> roles) {
        List<UserPropertyAndRole> createdRoles = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (UserPropertyAndRole role : roles) {
                try {
                    UserPropertyAndRole createdRole = addRole(uid, role);
                    createdRoles.add(createdRole);
                } catch (WebApplicationException e) {
                    log.trace("User {}, has this role  {} already. The role is not re-applied.", uid,role );
                }
            }
        }
        return roles;
    }

    public List<UserApplicationRoleEntry> addUserApplicationRoleEntries(String uid, List<UserApplicationRoleEntry> roles) {
        List<UserApplicationRoleEntry> createdRoles = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (UserApplicationRoleEntry role : roles) {
                try {
                    UserApplicationRoleEntry createdRole = addRole(uid, role);
                    createdRoles.add(createdRole);
                } catch (WebApplicationException e) {
                    log.trace("User {}, has this role  {} already. The role is not re-applied.", uid, role);
                }
            }
        }
        return roles;
    }

    @Deprecated
    public UserPropertyAndRole addRole(String uid, UserPropertyAndRole role) {
        if (userPropertyAndRoleDao.hasRole(uid, role)) {
            String msg = "User with uid=" + uid + " already has this role. " + role.toString();
            throw new WebApplicationException(msg, Response.Status.CONFLICT);
            //return role;
        }

        role.setUid(uid);
        userPropertyAndRoleDao.addUserPropertyAndRole(role);
        String value = "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + role.getApplicationRoleName();
        audit(ActionPerformed.ADDED, "role", value);
        return role;
    }

    public UserApplicationRoleEntry addRole(String uid, UserApplicationRoleEntry role) {
        if (userPropertyAndRoleDao.hasRole(uid, role)) {
            String msg = "User with uid=" + uid + " already has this role. " + role.toString();
            throw new WebApplicationException(msg, Response.Status.CONFLICT);
            //return role;
        }

        role.setUserId(uid);
        userPropertyAndRoleDao.addUserPropertyAndRole(role);
        String value = "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + role.getRoleName();
        audit(ActionPerformed.ADDED, "role", value);
        return role;
    }

    @Deprecated
    public UserPropertyAndRole addRoleIfNotExist(String uid, UserPropertyAndRole role) {
        if (userPropertyAndRoleDao.hasRole(uid, role)) {
            return role;
        }
        role.setUid(uid);
        userPropertyAndRoleDao.addUserPropertyAndRole(role);
        String value = "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + role.getApplicationRoleName();
        audit(ActionPerformed.ADDED, "role", value);
        return role;
    }

    public UserApplicationRoleEntry addRoleIfNotExist(String uid, UserApplicationRoleEntry role) {
        if (userPropertyAndRoleDao.hasRole(uid, role)) {
            return role;
        }
        role.setUserId(uid);
        userPropertyAndRoleDao.addUserPropertyAndRole(role);
        String value = "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + role.getRoleName();
        audit(ActionPerformed.ADDED, "role", value);
        return role;
    }

    public UserPropertyAndRole getRole(String uid, String roleId) {
        return getUserPropertyAndRole(roleId);
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntry(String uid, String roleId) {
        return getUserApplicationRoleEntry(roleId);
    }

    @Deprecated
    public UserPropertyAndRole getUserPropertyAndRole(String roleId) {
        UserPropertyAndRole role = userPropertyAndRoleDao.getUserPropertyAndRole(roleId);
        Application application = applicationDao.getApplication(role.getApplicationId());
        if (application != null) {
            role.setApplicationName(application.getName());
        }
        return role;
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntry(String roleId) {
        UserApplicationRoleEntry role = userPropertyAndRoleDao.getUserApplicationRoleEntry(roleId);
        Application application = applicationDao.getApplication(role.getApplicationId());
        if (application != null) {
            role.setApplicationName(application.getName());
        }
        return role;
    }

    @Deprecated
    public List<UserPropertyAndRole> getRoles(String uid) {
        return getUserPropertyAndRoles(uid);
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

    public List<UserApplicationRoleEntry> getUserApplicationRoleEntries(String uid) {
        List<UserApplicationRoleEntry> roles = userPropertyAndRoleDao.getUserApplicationRoleEntries(uid);
        for (UserApplicationRoleEntry role : roles) {
            Application application = applicationDao.getApplication(role.getApplicationId());
            if (application != null) {
                role.setApplicationName(application.getName());
            }
        }
        return roles;
    }

    @Deprecated
    public UserPropertyAndRole updateRole(String uid, String roleId, UserPropertyAndRole role) {
        UserPropertyAndRole existingUserPropertyAndRole = getUserPropertyAndRole(roleId);
        if (existingUserPropertyAndRole == null) {
            throw new NonExistentRoleException("RoleID does not exist: " + roleId);
        }
        if (!existingUserPropertyAndRole.getUid().equals(role.getUid())) {
            throw new InvalidRoleModificationException("Illegal attempt to change uid from " + existingUserPropertyAndRole.getUid() + " to " + role.getUid() + " for roleId " + roleId);
        }
        if (!existingUserPropertyAndRole.getApplicationId().equals(role.getApplicationId())) {
            throw new InvalidRoleModificationException("Illegal attempt to change applicationId from " + existingUserPropertyAndRole.getApplicationId() + " to " + role.getApplicationId() + " for roleId " + roleId);
        }
        if (!existingUserPropertyAndRole.getOrganizationName().equals(role.getOrganizationName())) {
            throw new InvalidRoleModificationException("Illegal attempt to change organizationName from " + existingUserPropertyAndRole.getOrganizationName() + " to " + role.getOrganizationName() + " for roleId " + roleId);
        }

        role.setUid(uid);
        role.setRoleId(roleId);
        userPropertyAndRoleDao.updateUserRoleValue(role);

        //audit(ActionPerformed.MODIFIED, "role", "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + jsonrole);
        return role;
    }

    public UserApplicationRoleEntry updateRole(String uid, String roleId, UserApplicationRoleEntry role) {
        UserApplicationRoleEntry existingUserApplicationRoleEntry = getUserApplicationRoleEntry(roleId);
        if (existingUserApplicationRoleEntry == null) {
            throw new NonExistentRoleException("RoleID does not exist: " + roleId);
        }
        if (!existingUserApplicationRoleEntry.getId().equals(role.getId())) {
            throw new InvalidRoleModificationException("Illegal attempt to change uid from " + existingUserApplicationRoleEntry.getId() + " to " + role.getId() + " for roleId " + roleId);
        }
        if (!existingUserApplicationRoleEntry.getApplicationId().equals(role.getApplicationId())) {
            throw new InvalidRoleModificationException("Illegal attempt to change applicationId from " + existingUserApplicationRoleEntry.getApplicationId() + " to " + role.getApplicationId() + " for roleId " + roleId);
        }
        if (!existingUserApplicationRoleEntry.getOrgName().equals(role.getOrgName())) {
            throw new InvalidRoleModificationException("Illegal attempt to change organizationName from " + existingUserApplicationRoleEntry.getOrgName() + " to " + role.getOrgName() + " for roleId " + roleId);
        }

        role.setUserId(uid);
        role.setId(roleId);
        userPropertyAndRoleDao.updateUserRoleValue(role);

        //audit(ActionPerformed.MODIFIED, "role", "uid=" + uid + ", appid=" + role.getApplicationId() + ", role=" + jsonrole);
        return role;
    }

    public void deleteRole(String uid, String roleid) {
        userPropertyAndRoleDao.deleteRole(roleid);
    }
}
