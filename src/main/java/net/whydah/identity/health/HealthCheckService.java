package net.whydah.identity.health;

import com.google.inject.Inject;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-13
 */
public class HealthCheckService {
    static final String USERADMIN_UID = "useradmin";    //uid of user which should always exist
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private final UserIdentityService identityService;
    private final UserPropertyAndRoleRepository roleRepository;

    @Inject
    public HealthCheckService(UserIdentityService identityService, UserPropertyAndRoleRepository roleRepository) {
        this.identityService = identityService;
        this.roleRepository = roleRepository;
    }


    boolean isOK() {
        log.trace("Checking if uid={} can be found in LDAP and role database.", USERADMIN_UID);
        //How to do count in ldap without fetching all users?
        if (!userExistInLdap(USERADMIN_UID)) {
            return false;
        }

        //if (!atLeastOneUserDatabase()) {
        //Can use count, but since ldap query need an UID, we can use UID here as well.
        if (!userExistInRoleDatabase(USERADMIN_UID)) {
            return false;
        }

        return true;
    }

    private boolean userExistInLdap(String uid) {
        try {
            UserIdentity user = identityService.getUserIdentityForUid(uid);
            if (user != null && uid.equals(user.getUid())) {
                return true;
            }
        } catch (NamingException e) {
            log.error("countUserRolesInDB failed. isOK returned false", e);
        }
        return false;
    }

    private boolean userExistInRoleDatabase(String uid) {
        List<UserPropertyAndRole> roles = roleRepository.getUserPropertyAndRoles(uid);
        if (roles.size() != 1) {
            log.error("Expected exactly one UserPropertyAndRole result for uid={}, but found {}", uid, roles.size());
            return false;
        }
        String uidFromRoleDB = roles.get(0).getUid();
        if (!uid.equals(uidFromRoleDB)) {
            log.error("Expected getUserPropertyAndRoles to return a user with uid={}, but uid from database was {}.", uid, uidFromRoleDB);
            return false;
        }
        return true;
    }
    /*
    private boolean atLeastOneUserDatabase() {
        try {
            if (roleRepository.countUserRolesInDB() > 0) {
                return true;
            }
        } catch (SQLException e) {
            log.error("countUserRolesInDB failed. isOK returned false", e);
        }
        return false;
    }
    */
}
