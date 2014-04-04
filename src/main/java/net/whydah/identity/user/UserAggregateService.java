package net.whydah.identity.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Singleton
public class UserAggregateService {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final UserIdentityService userIdentityService;
    private final UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    private final Indexer indexer;
    private final AuditLogRepository auditLogRepository;

    @Inject
    public UserAggregateService(UserIdentityService userIdentityService, UserPropertyAndRoleRepository userPropertyAndRoleRepository,
                                Indexer indexer, AuditLogRepository auditLogRepository) {
        this.indexer = indexer;
        this.auditLogRepository = auditLogRepository;
        this.userPropertyAndRoleRepository = userPropertyAndRoleRepository;
        this.userIdentityService = userIdentityService;
    }

    public UserAggregate addUserAndSetDefaultRoles(String userIdentityJson) {
        UserIdentity userIdentity = UserIdentity.fromJson(userIdentityJson);

        userIdentityService.addUserIdentity(userIdentity);

        List<UserPropertyAndRole> roles = addDefaultUserRole(userIdentity);

        indexer.addToIndex(userIdentity);

        audit(ActionPerformed.ADDED, "user", userIdentity.toString());
        return new UserAggregate(userIdentity, roles);
    }
    private List<UserPropertyAndRole> addDefaultUserRole(UserIdentity userIdentity) {
        UserPropertyAndRole defaultRole = new UserPropertyAndRole();

        String applicationId = AppConfig.appConfig.getProperty("adduser.defaultapplication.id");
        String applicationName = AppConfig.appConfig.getProperty("adduser.defaultapplication.name");
        String organizationId = AppConfig.appConfig.getProperty("adduser.defaultorganization.id");
        String organizationName = AppConfig.appConfig.getProperty("adduser.defaultorganization.name");
        String roleName = AppConfig.appConfig.getProperty("adduser.defaultrole.name");
        String roleValue = AppConfig.appConfig.getProperty("adduser.defaultrole.value");

        defaultRole.setUid(userIdentity.getUid());
        defaultRole.setApplicationId(applicationId);
        defaultRole.setApplicationName(applicationName);
        defaultRole.setOrgId(organizationId);
        defaultRole.setOrganizationName(organizationName);
        defaultRole.setApplicationRoleName(roleName);
        //role.setRoleValue(roleValue);
        defaultRole.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue
        //log.debug("Adding default role: {}", defaultRole);

        if (userPropertyAndRoleRepository.hasRole(userIdentity.getUid(), defaultRole)) {
            log.warn("Role already exist. Skip adding default role. Return existing roles instead. DefaultRole: " + defaultRole.toString());
            return userPropertyAndRoleRepository.getUserPropertyAndRoles(userIdentity.getUid());
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(defaultRole);
        String value = "uid=" + userIdentity + ", username=" + userIdentity.getUsername() + ", appid=" + defaultRole.getApplicationId() + ", role=" + defaultRole.getApplicationRoleName();
        audit(ActionPerformed.ADDED, "role", value);

        List<UserPropertyAndRole> roles = new ArrayList<>(1);
        roles.add(defaultRole);
        return roles;
    }


    public UserAggregate getUserAggregate(String username) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIndentity(username);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIndentity with username=" + username, e);
        }
        if (userIdentity == null) {
            log.trace("getUserAggregate could not find user with username={}", username);
            return null;
        }
        List<UserPropertyAndRole> userPropertyAndRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(userIdentity.getUid());
        return new UserAggregate(userIdentity, userPropertyAndRoles);
    }


    public UserIdentity updateUserIdentity(String username, String userJson) {
        UserIdentity newUserIdentity = UserIdentity.fromJson(userJson);

        try {
            UserIdentity userIdentity = userIdentityService.getUserIndentity(username);
            if (userIdentity == null) {
                return null;
            }

            userIdentityService.updateUserIdentity(username, newUserIdentity);
            indexer.update(newUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", newUserIdentity.toString());
        } catch (NamingException e) {
            throw new RuntimeException("updateUserIdentity failed for username=" + username + ", newUserIdentity=" + newUserIdentity, e);
        }
        return newUserIdentity;
    }

    public void deleteUserAggregate(String username) {
        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIndentity(username);
        } catch (NamingException e) {
            throw new RuntimeException("userIdentityService.getUserIndentity with username=" + username, e);
        }
        if (userIdentity == null) {
            throw new IllegalArgumentException("UserIdentity not found. username=" + username);
        }
        userIdentityService.deleteUserIdentity(username);

        String uid = userIdentity.getUid();
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
