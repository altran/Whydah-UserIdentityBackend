package net.whydah.identity.user.signup;

import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.resource.UserAggregateRepresentation;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 30.09.15.
 */
@Service
public class UserSignupService {
    private static final Logger log = getLogger(UserSignupService.class);

    private final UserAggregateService userAggregateService;
    private final UserIdentityService userIdentityService;

    @Autowired
    public UserSignupService(UserAggregateService userAggregateService, UserIdentityService userIdentityService) {
        this.userAggregateService = userAggregateService;
        this.userIdentityService = userIdentityService;
    }

    public UserAggregate createUserWithRoles(UserAggregateRepresentation createFromRepresentation) {
        UserAggregate userAggregate = null;
        if (createFromRepresentation != null) {
            UserAggregate createFromAggregate = createFromRepresentation.buildUserAggregate();
            UserIdentity createFromItentity = createFromAggregate.getIdentity();
            UserIdentity userIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(createFromItentity);
            //Add roles
            if (userIdentity != null && userIdentity.getUid() != null) {
                List<UserPropertyAndRole> roles = createFromAggregate.getRoles();
                String uid = userIdentity.getUid();
                List<UserPropertyAndRole> createdRoles = userAggregateService.addRoles(uid, roles);
                userAggregate = new UserAggregate(userIdentity, createdRoles);
            }
        }
        return userAggregate;
    }
}
