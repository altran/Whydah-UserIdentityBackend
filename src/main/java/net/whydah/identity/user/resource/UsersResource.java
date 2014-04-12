package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.user.UserAggregate;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;

/**
 * Endpoint for collection of users.
 */
@Path("/{applicationtokenid}/{usertokenid}/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);

    private final UserAggregateService userAggregateService;
    private final Search search;

    @Context
    private UriInfo uriInfo;

    @Inject
    public UsersResource(Search search, UserAggregateService userAggregateService) {
        this.search = search;
        this.userAggregateService = userAggregateService;
    }

    /**
     * Get user details.
     *
     * @param username Username
     * @return user details and roles.
     */
    @GET
    @Path("/username/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAggregate(@PathParam("username") String username) {
        log.trace("getUserAggregateByUsername with username=" + username);

        UserAggregate user;
        try {
            user = userAggregateService.getUserAggregateByUsername(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        HashMap<String, Object> model = new HashMap<>(2);
        model.put("user", user);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/user.json.ftl", model)).build();
    }


    /**
     * Find users.
     *
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("q") String query) {
        log.debug("find with query=" + query);
        List<UserIdentityRepresentation> result = search.search(query);

        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", result);
        log.info("users", result);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/users.json.ftl", model)).build();
    }
}
