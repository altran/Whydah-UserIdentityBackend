package net.whydah.identity.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Endpoint for health check.
 */
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response isHealthy() {
        log.trace("isHealthy");
        return Response.ok("Status OK!").build();
        //return Response.status(Response.Status.NO_CONTENT).build();
    }
}
