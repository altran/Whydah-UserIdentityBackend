package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.user.identity.WhydahUserIdentity;
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
@Path("/useradmin/{usertokenid}/")
public class UsersResource {
    private static final Logger logger = LoggerFactory.getLogger(UsersResource.class);

    private Search search;

    @Context
    private UriInfo uriInfo;

    @Inject
    public UsersResource(Search search) {
        this.search = search;
    }

    /**
     * Find users.
     *
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("find/{q}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("q") String query) {
        logger.debug("find with query=" + query);
        List<WhydahUserIdentity> result = search.search(query);

        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", result);
        logger.info("users", result);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/users.json.ftl", model)).build();
    }
}
