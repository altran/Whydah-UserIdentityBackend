package net.whydah.identity.application;

import com.google.inject.Inject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * Created by baardl on 29.03.14.
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();


    @Inject
    public ApplicationResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Create a new application from json
     * Add default
     *
     * @param applicationJson  json representing an Application
     * @return  Application
     */
    @POST
     @Path("/")
     @Consumes(MediaType.APPLICATION_JSON)
     public Response createApplication(String applicationJson)  {
        log.trace("createApplication is called with applicationJson={}", applicationJson);
        Application application;
        try {
            application = applicationService.createApplication(applicationJson);
            //return Response.status(Response.Status.OK).build();

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

        if (application != null) {
            String applicationCreatedJson = buildApplicationJson(application);
            return Response.ok(applicationCreatedJson).build();
        } else {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    @GET
    @Path("/{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplication(@PathParam("applicationId") String applicationId){
        log.trace("getApplication is called with applicationId={}", applicationId);
        try {
            Application application = applicationService.getApplication(applicationId);
            String applicationCreatedJson = buildApplicationJson(application);
            return Response.ok(applicationCreatedJson).build();
        } catch (IllegalArgumentException iae) {
            log.error("createApplication: Invalid json={}", applicationId, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    protected String buildApplicationJson(Application application) {
        String applicationCreatedJson = null;
        try {
            applicationCreatedJson = mapper.writeValueAsString(application);
        } catch (IOException e) {
            log.warn("Could not convert application to Json {}", application.toString());
        }
        return applicationCreatedJson;
    }

}
