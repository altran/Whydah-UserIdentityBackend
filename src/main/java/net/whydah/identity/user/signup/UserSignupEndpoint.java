package net.whydah.identity.user.signup;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.audit.AuditLogDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.resource.UserAggregateRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Service for authorization of users and finding UserAggregate with corresponding applications, organizations and roles.
 * This not a RESTful endpoint. This is a http RPC endpoint.
 */
@Component
@Path("/{applicationTokenId}/signup")
public class UserSignupEndpoint {
    private static final Logger log = LoggerFactory.getLogger(UserSignupEndpoint.class);

//    private final UserAggregateService userAggregateService;
//    private final UserAdminHelper userAdminHelper;
    private final UserIdentityService userIdentityService;
    private final ObjectMapper objectMapper;
    //private final String hostname;


    private final AuditLogDao auditLogDao;

    @Autowired
    public UserSignupEndpoint(UserIdentityService userIdentityService, ObjectMapper objectMapper, AuditLogDao auditLogDao) {
        this.userIdentityService = userIdentityService;
        this.objectMapper = objectMapper;
        this.auditLogDao = auditLogDao;
    }

    /**
     * Signup using json.  Format
     {"username":"helloMe", "firstName":"hello", "lastName":"me", "personRef":"", "email":"hello.me@example.com", "cellPhone":"+47 90221133"}
     username is required
     */
    @Path("/user")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signupUser(String userJson) {
        log.trace("signupUser: {}", userJson);
        UserAggregateRepresentation userAggregate = null;
        try {
            userAggregate = objectMapper.readValue(userJson, UserAggregateRepresentation.class);
        } catch (IOException ioe) {
            log.trace("Failed to parse UserAggregateRepresentation from json {}", userJson);
        }

        if (userAggregate == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse " + userJson + ".").build();
        }

        UserIdentity userIdentity = null; //TODO BLI UserAdminHelper.createWhydahUserIdentity(fbUserDoc);

        if (userIdentity == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }


        /*
        String facebookUserAsString = getFacebookDataAsXmlString(fbUserDoc);
        //String facebookUserAsString = getFacebookDataAsXmlString(input);
        return createAndAuthenticateUser(userIdentity, facebookUserAsString, true);
        */
        return  null;
    }


}
