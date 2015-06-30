package net.whydah.identity.application;

import net.whydah.sso.application.Application;
import net.whydah.sso.application.ApplicationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by baardl on 29.03.14.
 */
@Path("/{applicationtokenid}/{userTokenId}/application")
public class ApplicationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private final ApplicationService applicationService;

    @Autowired
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
        log.trace("create is called with applicationJson={}", applicationJson);
        Application application;
        try {
            application = ApplicationSerializer.fromJson(applicationJson);
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Application persisted;
        try {
            persisted = applicationService.create(application);
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
            
        /*
        try {
            application = applicationService.create(applicationJson);
            //return Response.status(Response.Status.OK).build();
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        */

        if (persisted != null) {
            String json = ApplicationSerializer.toJson(persisted);
            return Response.ok(json).build();
        } else {
            //TODO If it was not persisted, should return error message
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
            String json = ApplicationSerializer.toJson(application);
            return Response.ok(json).build();
        } catch (IllegalArgumentException iae) {
            log.error("create: Invalid json={}", applicationId, iae);
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
