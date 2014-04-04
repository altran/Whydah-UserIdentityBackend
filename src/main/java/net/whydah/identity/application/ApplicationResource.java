package net.whydah.identity.application;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by baardl on 29.03.14.
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    ApplicationService applicationService;


    @Inject
    public ApplicationResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Create a new applcation from json
     * Add default
     *
     * @param applicationJson  json representing an Application
     * @return  Application
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(String applicationJson){
        log.trace("createApplication is called with applicationJson={}", applicationJson);
        try {
            Application application = applicationService.createApplication(applicationJson);
            return Response.status(Response.Status.OK).build();
        } catch (IllegalArgumentException iae) {
            log.error("createApplication: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
