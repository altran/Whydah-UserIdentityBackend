package net.whydah.identity.application;

import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-11-21.
 */
@Component
@Path("/{stsApplicationtokenId}/application/auth")
public class ApplicationAuthenticationEndpoint {
    static final String UAS_APP_CREDENTIAL_XML = "uasAppCredentialXml";
    static final String APP_CREDENTIAL_XML = "appCredentialXml";
    private static final Logger log = LoggerFactory.getLogger(ApplicationAuthenticationEndpoint.class);

    private final ApplicationService applicationService;

    @Autowired
    public ApplicationAuthenticationEndpoint(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    /**
     * Authenticate 3. party application using app credential
     * @param stsApplicationtokenId not in use, expected to be useful in the future
     * @param uasAppCredentialXml   application credential for UAS, only UAS is allowed to communicate with UIB
     * @param appCredentialXml  application credential for application to authenticate
     * @return  204 No Content if successful, otherwise 401 Forbidden
     */
    @POST
    //@Path("/{stsApplicationtokenId}/application/auth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    //@Produces(MediaType.TEXT_PLAIN)
    public Response authenticateApplication(@PathParam("stsApplicationtokenId") String stsApplicationtokenId,
                                            @FormParam(UAS_APP_CREDENTIAL_XML) String uasAppCredentialXml,
                                            @FormParam(APP_CREDENTIAL_XML) String appCredentialXml) {

        //verify uasAppCredentialXml
        ApplicationCredential uasAppCredential = ApplicationCredentialMapper.fromXml(uasAppCredentialXml);
        Application uasApplication = applicationService.authenticate(uasAppCredential);
        if (uasApplication == null) {
            log.debug("Application authentication failed for {}. Returning {}", uasAppCredential, Response.Status.FORBIDDEN);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        //verify appCredential for application to verify
        ApplicationCredential appCredentialToVerify = ApplicationCredentialMapper.fromXml(appCredentialXml);
        Application verifiedApplication = applicationService.authenticate(appCredentialToVerify);
        if (verifiedApplication == null) {
            log.debug("Application authentication failed for {}. Returning {}", appCredentialToVerify, Response.Status.FORBIDDEN);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        log.info("Authentication ok for {}", verifiedApplication);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
