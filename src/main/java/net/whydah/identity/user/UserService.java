package net.whydah.identity.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.LDAPHelper;
import net.whydah.identity.user.identity.UserAuthenticationService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import net.whydah.identity.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 29.03.14
 */
@Singleton
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final LDAPHelper ldapHelper;
    private final Indexer indexer;
    private final AuditLogRepository auditLogRepository;
    private final UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    private final PasswordGenerator passwordGenerator;
    private final UserAuthenticationService userAuthenticationService;


    @Inject
    public UserService(LDAPHelper ldapHelper, Indexer indexer, AuditLogRepository auditLogRepository,
                       UserPropertyAndRoleRepository userPropertyAndRoleRepository, PasswordGenerator passwordGenerator,
                       UserAuthenticationService userAuthenticationService) {
        this.ldapHelper = ldapHelper;
        this.indexer = indexer;
        this.auditLogRepository = auditLogRepository;
        this.userPropertyAndRoleRepository = userPropertyAndRoleRepository;
        this.passwordGenerator = passwordGenerator;
        this.userAuthenticationService = userAuthenticationService;
    }

    public WhydahUserIdentity addUser(String userJson) {
        WhydahUserIdentity userIdentity = WhydahUserIdentity.fromJson(userJson);
        userIdentity.setPassword(passwordGenerator.generate());

        String username = userIdentity.getUsername();
        //try {
        try {
            if (ldapHelper.usernameExist(username)) {
                //log.info("User already exists, could not create user " + username);
                //return Response.status(Response.Status.NOT_ACCEPTABLE).build();
                throw new IllegalStateException("User already exists, could not create user " + username);
            }
        } catch (NamingException e) {
            throw new RuntimeException("usernameExist failed for username=" + username, e);
        }

        userIdentity.setUid(UUID.randomUUID().toString());
        try {
            ldapHelper.addWhydahUserIdentity(userIdentity);
        } catch (NamingException e) {
            throw new RuntimeException("addWhydahUserIdentity failed for " + userIdentity.toString(), e);
        }
        log.info("Added new user to LDAP: {}", username);
        /*
        } catch (Exception e) {
            log.error("Could not create user " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        */

        addDefaultWhydahUserRole(userIdentity);

        //try {
        indexer.addToIndex(userIdentity);
        audit(ActionPerformed.ADDED, "user", userIdentity.toString());
            /*
        } catch (Exception e) {
            log.error("Error with lucene indexing or audit loggin for " + username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        */
        //return Response.ok().build();
        return userIdentity;
    }

    public WhydahUser getUser(String username) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            throw new RuntimeException("userAuthenticationService.getUserinfo with username=" + username, e);
        }
        if (whydahUserIdentity == null) {
            log.trace("getUser could not find user with username={}", username);
            return null;
        }
        List<UserPropertyAndRole> userPropertyAndRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        return new WhydahUser(whydahUserIdentity, userPropertyAndRoles);
    }

    public WhydahUserIdentity modifyUserIdentity(String username, String userJson) {
        WhydahUserIdentity newUserIdentity = WhydahUserIdentity.fromJson(userJson);

        try {
            WhydahUserIdentity whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            if (whydahUserIdentity == null) {
                return null;
            }

            userAuthenticationService.updateUser(username, newUserIdentity);
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
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            throw new RuntimeException("userAuthenticationService.getUserinfo with username=" + username, e);
        }
        if (whydahUserIdentity == null) {
            throw new IllegalArgumentException("UserIdentity not found. username=" + username);
        }

        userAuthenticationService.deleteUser(username);
        String uid = whydahUserIdentity.getUid();
        userPropertyAndRoleRepository.deleteUser(uid);
        indexer.removeFromIndex(uid);
        audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + username);
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
//        role.setRoleValue(roleValue);
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
