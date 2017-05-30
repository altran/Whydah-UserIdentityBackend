package net.whydah.identity.user.resource;


import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.search.UserSearch;
import net.whydah.sso.user.types.UserIdentity;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
@Path("/{applicationtokenid}/{usertokenid}/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);
    private final UserSearch userSearch;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UsersResource(UserSearch userSearch) {
        this.userSearch = userSearch;
    }

    /**
     * Find users.
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<UserIdentity> users = userSearch.search(query);
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", users);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", users.size());
        Response response = Response.ok(new Viewable("/useradmin/users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

    /**
     * Lookup users in directory  (Intermediate copy of find)
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/search/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response userSearch(@PathParam("q") String query) {
        return findUsers(query);
    }
}
