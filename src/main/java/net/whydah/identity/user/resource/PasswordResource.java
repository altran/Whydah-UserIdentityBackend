package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Path("/password/{applciationtokenid}")
public class PasswordResource {
    private static final Logger log = LoggerFactory.getLogger(PasswordResource.class);

    private final UserIdentityService userIdentityService;

    @Context
    private UriInfo uriInfo;

    @Inject
    public PasswordResource(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
        log.info("Started: PasswordResource");
    }

    @GET
    @Path("/reset/username/{username}")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for user {}", username);
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userIdentityService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Path("/reset/username/{username}/newpassword/{token}")
    public Response setPassword(@PathParam("username") String username,@PathParam("token") String token,String passwordJson) {
        log.info("newpassword for user {} token {}", username,token);
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }
            boolean ok;
            try {
                ok = userIdentityService.authenticateWithChangePasswordToken(username, token);
            } catch (RuntimeException re) {
                log.error("changePasswordForUser-RuntimeException username {}, message {}", username,re.getMessage(), re);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                userIdentityService.changePassword(username, user.getUid(), newpassword);
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            return Response.ok().build();
        } catch (Exception e) {
            log.error("newpassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
