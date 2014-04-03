package net.whydah.identity.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Singleton
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final UserIdentityService userIdentityService;
    private final UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    private final Indexer indexer;
    private final AuditLogRepository auditLogRepository;

    @Inject
    public UserService(UserIdentityService userIdentityService, UserPropertyAndRoleRepository userPropertyAndRoleRepository,
                       Indexer indexer, AuditLogRepository auditLogRepository) {
        this.indexer = indexer;
        this.auditLogRepository = auditLogRepository;
        this.userPropertyAndRoleRepository = userPropertyAndRoleRepository;
        this.userIdentityService = userIdentityService;
    }

    public WhydahUserIdentity addUser(String userJson) {
        WhydahUserIdentity userIdentity = WhydahUserIdentity.fromJson(userJson);

        userIdentityService.addUserToLdap(userIdentity);

        addDefaultWhydahUserRole(userIdentity);

        indexer.addToIndex(userIdentity);

        audit(ActionPerformed.ADDED, "user", userIdentity.toString());
        return userIdentity;
    }
    private void addDefaultWhydahUserRole(WhydahUserIdentity userIdentity) {
        UserPropertyAndRole role = new UserPropertyAndRole();

        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String roleValue = AppConfig.appConfig.getProperty("adduser.defaultrole.value");

        role.setUid(userIdentity.getUid());
        role.setAppId(applicationId);
        role.setApplicationName(applicationName);
        role.setOrgId(organizationId);
        role.setOrganizationName(organizationName);
        role.setRoleName(roleName);
        //role.setRoleValue(roleValue);
        role.setRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
        log.debug("Adding Role: {}", role);

        if (userPropertyAndRoleRepository.hasRole(userIdentity.getUid(), role)) {
            log.warn("Role already exist. " + role.toString());
            return;
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + role.getAppId() + ", role=" + role.getRoleName();
        audit(ActionPerformed.ADDED, "role", value);
    }


    public WhydahUser getUser(String username) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserinfo with username=" + username, e);
        }
        if (whydahUserIdentity == null) {
            log.trace("getUser could not find user with username={}", username);
            return null;
        }
        List<UserPropertyAndRole> userPropertyAndRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        return new WhydahUser(whydahUserIdentity, userPropertyAndRoles);
    }


    public WhydahUserIdentity updateUserIdentity(String username, String userJson) {
        WhydahUserIdentity newUserIdentity = WhydahUserIdentity.fromJson(userJson);

        try {
            WhydahUserIdentity whydahUserIdentity = userIdentityService.getUserinfo(username);
            if (whydahUserIdentity == null) {
                return null;
            }

            userIdentityService.updateUser(username, newUserIdentity);
            indexer.update(newUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
        } catch (NamingException e) {
            throw new RuntimeException("updateUser failed for username=" + username + ", newUserIdentity=" + newUserIdentity, e);
        }
        return newUserIdentity;
    }

    public void deleteUser(String username) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserinfo with username=" + username, e);
        }
        if (whydahUserIdentity == null) {
            throw new IllegalArgumentException("UserIdentity not found. username=" + username);
        }

        userIdentityService.deleteUser(username);

        String uid = whydahUserIdentity.getUid();
        userPropertyAndRoleRepository.deleteUser(uid);
        indexer.removeFromIndex(uid);
        audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + username);
    }

    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            log.error("authenticatedUser is not set. Auditing failed for action=" + action + ", what=" + what + ", value=" + value);
            return;
        }
        String userId = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(userId, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }
}
