package net.whydah.identity.application;

import com.google.inject.Inject;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.authentication.UserToken;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.search.UserSearch;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * @author Stig@Lau.no
 * Endpoint for testing and determining whether UIB can connect appropriately to its integration endpoints and retrieve sane results
 */
@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
public class TestResource {
    private static final Logger logger = LoggerFactory.getLogger(TestResource.class);
    ObjectMapper mapper = new ObjectMapper();

    @Inject
    ApplicationService applicationService;

    @Inject
    SecurityTokenServiceHelper tokenService;

    @Inject
    UserSearch userSearch;

    @GET
    @Path("/")
    public Response info() {
        String availablePaths = "";
        availablePaths += "/uib/test/db/applications";
        availablePaths += "/uib/test/ldap/numberofusers";
        availablePaths += "/uib/test/securitytokenservice";
        //availablePaths.add("/uib/test/index/users");
        return Response.ok(buildApplicationJson(availablePaths)).build();
    }

    @GET
    @Path("/db/applications")
    public Response applications() {
        List<Application> applications = applicationService.getApplications();
        return Response.ok("Applications in DB: " + applications.size()).build();
    }

    @GET
    @Path("/ldap/numberofusers")
    public Response ldapusers() {
        try {
            List<UserIdentityRepresentation> users = userSearch.search("(initials=*)");
            Integer nrOfUsers = users.size();
            return Response.ok(buildApplicationJson(nrOfUsers)).build();
        }catch(Exception e) {
            logger.error("Connecting to LDAP failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Connecting to LDAP failed. See log for more info: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/securitytokenservice")
    public Response tokenService() {
        String errorMessage = "";
        try {
            UserToken tokenServiceReply = tokenService.getUserToken("nonFunctionalAppTokenId", "nonFunctionalUserTokenId");
            if (tokenServiceReply != null) {
                return Response.ok(tokenServiceReply).build();
            }
        }catch(Exception e) {
            errorMessage = e.getMessage();
            logger.error("Connecting to Token Service failed", e);
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Connecting to Token Service failed. See log for more info: " + errorMessage).build();
    }


    protected String buildApplicationJson(Object thingToConvertToJson) {
        String applicationCreatedJson = null;
        try {
            applicationCreatedJson = mapper.writeValueAsString(thingToConvertToJson);
        } catch (IOException e) {
            logger.warn("Could not convert application to Json {}", thingToConvertToJson.toString());
        }
        return applicationCreatedJson;
    }
}
