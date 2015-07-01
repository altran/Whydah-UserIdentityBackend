package net.whydah.identity.application.authentication;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.Application;
import net.whydah.sso.application.ApplicationCredential;
import net.whydah.sso.application.ApplicationCredentialSerializer;
import net.whydah.sso.application.ApplicationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-01
 */
@Path("/application/authenticate")
public class ApplicationAuthenticationResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationAuthenticationResource.class);
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationAuthenticationResource(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * @param input ApplicationCredential as XML
     * @return  JSON representation of Application upon valid authentication.
     */
    @POST
    @Consumes(MediaType.APPLICATION_XML)    //Might consider using json for input data as well.
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticateApplication(InputStream input) {
        log.trace("authenticateApplication from XML InputStream");
        try {
            ApplicationCredential credential = ApplicationCredentialSerializer.fromXml(input);
            Application application = applicationService.authenticate(credential);
            if (application != null) {
                String json = ApplicationSerializer.toJson(application);
                return Response.ok(json).build();
            }
        } catch (IllegalArgumentException pe) {
            log.warn(pe.getMessage(), pe.getCause());
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid xml").build();
        } catch (RuntimeException e) {
            log.error("", e);
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}
