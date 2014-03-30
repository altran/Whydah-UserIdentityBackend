package net.whydah.identity.applicationtoken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by baardl on 22.03.14.
 */
@Path("/applicationtoken")
public class ApplicationTokenResource {
    private final static Logger log = LoggerFactory.getLogger(ApplicationTokenResource.class);

    //TODO baardl preparing for Application Authorization.
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateApplication(@QueryParam("appCredentialXml") String appCredentialXml){
        log.trace("authenticateApplication {}", appCredentialXml);
        //FIXME real implementation of ApplicationTokenResource.
        ApplicationToken applicationToken = new ApplicationToken();
        log.warn("Accessing insecure mock. Application Validation is Ommited!");
        return Response.status(Response.Status.OK).entity(applicationToken.toXML()).build();
    }
}
