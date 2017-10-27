package net.whydah.identity.health;

import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.util.WhydahUtil;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

/**
 * Endpoint for health check.
 */
@Component
@Path("/health")
public class HealthResource {
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private final HealthCheckService healthCheckService;
    private static SecurityTokenServiceClient securityTokenServiceClient;
    private static String applicationInstanceName;
    private static boolean ok = true;


    private static long numberOfUsers = 0;

    private static long numberOfApplications = 0;

    @Autowired
    @Configure
    public HealthResource(SecurityTokenServiceClient securityTokenHelper, HealthCheckService healthCheckService, @Configuration("applicationname") String applicationname) {
        this.securityTokenServiceClient = securityTokenHelper;
        this.healthCheckService = healthCheckService;
        this.applicationInstanceName = applicationname;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isHealthy() {
        ok = healthCheckService.isOK();
        String statusText = WhydahUtil.getPrintableStatus(securityTokenServiceClient.getWAS());
        log.trace("isHealthy={}, {status}", ok, statusText);
        if (ok) {
            //return Response.status(Response.Status.NO_CONTENT).build();
            return Response.ok(getHealthTextJson()).build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("intrusions")
    public Response countIntrusions() {
        long intrusions = healthCheckService.countIntrusionAttempts();
        long anonymousIntrusions = healthCheckService.countAnonymousIntrusionAttempts();

        return Response.ok("{\"intrusionAttempt\":" + intrusions + ",\"anonymousIntrusionAttempt\":" + anonymousIntrusions + "}").build();

    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"" + ok + "\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + SecurityTokenServiceClient.was.getDefcon() + "\",\n" +
                "  \"STS\": \"" + SecurityTokenServiceClient.was.getSTS() + "\",\n" +
                "  \"hasApplicationToken\": \"" + Boolean.toString(SecurityTokenServiceClient.was.getActiveApplicationTokenId() != null) + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + Boolean.toString(SecurityTokenServiceClient.was.checkActiveSession()) + "\",\n" +
                "  \"users\": \"" + numberOfUsers + "\",\n" +
                "  \"applications\": \"" + numberOfApplications + "\",\n" +
                "  \"now\": \"" + Instant.now() + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +
                "  \"intrusionAttemptsDetected\": " + healthCheckService.countIntrusionAttempts() + ",\n" +
                "  \"anonymousIntrusionAttemptsDetected\": " + healthCheckService.countAnonymousIntrusionAttempts() + "\n" +
                "}\n";
    }


    public static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.identity/UserIdentityBackend/pom.properties";
        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }

    public static long getNumberOfUsers() {
        return numberOfUsers;
    }

    public static void setNumberOfUsers(long numberOfUsers) {
        HealthResource.numberOfUsers = numberOfUsers;
    }

    public static long getNumberOfApplications() {
        return numberOfApplications;
    }

    public static void setNumberOfApplications(long numberOfApplications) {
        HealthResource.numberOfApplications = numberOfApplications;
    }


}

