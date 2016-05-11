package net.whydah.identity.application;

import net.whydah.identity.application.search.ApplicationSearch;
import net.whydah.identity.application.search.LuceneApplicationIndexer;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.lucene.store.NIOFSDirectory;
import org.constretto.ConstrettoConfiguration;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Component
@Path("/{applicationtokenid}/")
public class ApplicationsResource {
    private static final Logger log = LoggerFactory.getLogger(ApplicationsResource.class);
    private final ApplicationService applicationService;
    private final ApplicationSearch applicationSearch;
    private final LuceneApplicationIndexer luceneApplicationIndexer;

    //@Autowired
    //public ApplicationsResource(ApplicationService applicationService) {
    //    this.applicationService = applicationService;
    //}

    @Autowired
    public ApplicationsResource(ApplicationService applicationService, ApplicationSearch applicationSearch, ConstrettoConfiguration configuration) {
        this.applicationService = applicationService;
        this.applicationSearch = applicationSearch;
        String luceneApplicationDir = configuration.evaluateToString("lucene.directory");
        NIOFSDirectory index = createDirectory(luceneApplicationDir);
        luceneApplicationIndexer = new LuceneApplicationIndexer(index);

    }

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(){
        log.trace("getApplications is called.");
        try {
            List<Application> applications = applicationService.getApplications();
            if (applications != null && applications.size() > 0) {
                log.debug("application [0] - {}",applications.get(0).toString());
            }
            String json = ApplicationMapper.toSafeJson(applications);
            log.trace("Returning {} applications: {}", applications.size(), json);
            return Response.ok(json).build();
        } catch (RuntimeException e) {
            log.error("getApplications failed.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Find users.
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/{userTokenId}/applications/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<Application> applications = applicationSearch.search(query);
        String json = ApplicationMapper.toSafeJson(applications);
        log.trace("Returning {} applications: {}", applications.size(), json);
        Response response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

    private NIOFSDirectory createDirectory(String luceneDir) {
        try {
            File luceneDirectory = new File(luceneDir);
            if (!luceneDirectory.exists()) {
                boolean dirsCreated = luceneDirectory.mkdirs();
                if (!dirsCreated) {
                    log.debug("{} was not successfully created.", luceneDirectory.getAbsolutePath());
                }
            }
            return new NIOFSDirectory(luceneDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

