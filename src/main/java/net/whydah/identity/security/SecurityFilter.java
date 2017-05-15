package net.whydah.identity.security;

import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.health.HealthCheckService;
import net.whydah.identity.user.authentication.SecurityTokenServiceClient;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes authentication.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
@Component
public class SecurityFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    public static final String APPLICATION_CREDENTIALS_HEADER_XML = "uas-app-credentials/xml";
    public static final String pwPattern = "/user/.+/(reset|change)_password";
    // /password/6f485dd168bb999c7fb9696c75fad3c3/reset/username/totto@cantara.no
//    public static final String pwPattern2 = "/password/(.*)/reset/username/(.*)";
    public static final String pwPattern2 = "(.*)/reset/username/(.*)";
    public static final String pwPattern3 = "/user/.+/password_login_enabled";
    public static final String userAuthPattern = "/authenticate/user(|/.*)";
    public static final String applicationAuthPatten = "/application/auth";
    public static final String applicationListPatten = "/applications";
    public static final String userSignupPattern = "/signup/user";
    public static final String[] patternsWithoutUserTokenId = {applicationAuthPatten, pwPattern, pwPattern2, pwPattern3, userAuthPattern, userSignupPattern, applicationListPatten};
    public static final String HEALT_PATH = "/health";

    private final SecurityTokenServiceClient securityTokenHelper;
    private final AuthenticationService authenticationService;
    private final HealthCheckService healthCheckService;

    private static boolean isCI = false;
    //public static final String OPEN_PATH = "/authenticate";
    //public static final String AUTHENTICATE_USER_PATH = "/authenticate";
    //public static final String PASSWORD_RESET_PATH = "/password";
    //public static final String SECURED_PATHS_PARAM = "securedPaths";
    //public static final String REQUIRED_ROLE_USERS = "WhydahUserAdmin";
    //public static final String REQUIRED_ROLE_APPLICATIONS = "WhydahUserAdmin";
    //private List<String> securedPaths = new ArrayList<>();
    //private String requiredRole;

    @Autowired
    public SecurityFilter(SecurityTokenServiceClient securityTokenHelper, AuthenticationService authenticationService, HealthCheckService healthCheckService) {
        this.securityTokenHelper = securityTokenHelper;
        this.authenticationService = authenticationService;
        this.healthCheckService = healthCheckService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //requiredRole = REQUIRED_ROLE_USERS;
        SecurityFilter.setCIFlag(true);
    }


    /**
     * @param pathInfo the path to apply the filter to
     * @return HttpServletResponse.SC_UNAUTHORIZED if authentication fails, otherwise null
     */
    Integer authenticateAndAuthorizeRequest(String pathInfo) {
        log.debug("filter path {}", pathInfo);

        //match /
        if (pathInfo == null || pathInfo.equals("/")) {
            return HttpServletResponse.SC_NOT_FOUND;
        }

        String path = pathInfo.substring(1); //strip leading /

        //Open paths without authentication
        if (path.startsWith(HEALT_PATH)) {
            log.debug("{} was matched to {}. SecurityFilter passed.", path, HEALT_PATH);
            return null;
        }

        if (ApplicationMode.skipSecurityFilter()) {
            log.warn("Running in noSecurityFilter mode, security is omitted for users.");
            Authentication.setAuthenticatedUser(buildMockedUserToken());
            return null;
        }


        // /{stsApplicationtokenId}/application/auth        //ApplicationAuthenticationEndpoint


        //strip applicationTokenId from pathInfo
        path = path.substring(path.indexOf("/"));


        //paths without userTokenId verification
        /*
        /{applicationTokenId}/user/{uid}/reset_password     //PasswordResource2
        /{applicationTokenId}/user/{uid}/change_password    //PasswordResource2
        /{applicationTokenId}/authenticate/user/*           //UserAuthenticationEndpoint
        /{stsApplicationtokenId}/application/auth")         //Applicationcredential verification endpoint  (ApplicationAuthenticationEndpoint)
        /{applicationTokenId}/signup/user                   //UserSignupEndpoint
        */
        for (String pattern : patternsWithoutUserTokenId) {
            if (Pattern.compile(pattern).matcher(path).matches()) {
                log.debug("{} was matched to {}. SecurityFilter passed.", path, pattern);
                return null;
            }
        }


        /*
        /{applicationtokenid}/{userTokenId}/application     //ApplicationResource
        /{applicationtokenid}/{userTokenId}/applications    //ApplicationsResource
        /{applicationtokenid}/{userTokenId}/user            //UserResource
        /{applicationtokenid}/{usertokenid}/useraggregate   //UserAggregateResource
        /{applicationtokenid}/{usertokenid}/users           //UsersResource
         */
        //paths WITH userTokenId verification
        String pathElement1 = findPathElement(pathInfo, 1);
        String applicationTokenId = findPathElement(pathInfo, 1).substring(1);
        String usertokenId = findPathElement(pathInfo, 2).substring(1);
        if (applicationTokenId != null) {
            // Not calling myself
            if (!applicationTokenId.equalsIgnoreCase(securityTokenHelper.getActiveUibApplicationTokenId())) {
                UserToken userToken = securityTokenHelper.getUserToken(usertokenId);
                if (userToken == null || userToken.toString().length() < 10) {
                    return HttpServletResponse.SC_UNAUTHORIZED;
                }

                // We are happy here :)
                Authentication.setAuthenticatedUser(userToken);
            } else {
                log.info("UIB is calling itself - shuld be OK");
            }
        } else {
            log.warn("Missing applicationTokenId in request ");
            return HttpServletResponse.SC_UNAUTHORIZED;
        }


        return null;
    }

    UserToken buildMockedUserToken() {
        UserApplicationRoleEntry role = new UserApplicationRoleEntry();
        role.setApplicationName("mockrole");
        role.setApplicationId("9999");
        role.setId("99999");
        UserToken mockToken = new UserToken();
        mockToken.setUserName("MockUserToken");
        mockToken.addApplicationRoleEntry(role);
        return mockToken;
    }

    protected String findPathElement(String pathInfo, int elementNumber) {
        String pathElement = null;
        if (pathInfo != null) {
            String[] pathElements = pathInfo.split("/");
            if (pathElements.length > elementNumber) {
                pathElement = "/" + pathElements[elementNumber];
            }
        }
        return pathElement;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        String applicationCredentialXmlEncoded = servletRequest.getHeader(APPLICATION_CREDENTIALS_HEADER_XML);
        boolean isUas = false;
        //Enable tests to pass through.
        if (ApplicationMode.skipSecurityFilter()) {
            log.warn("Running in noSecurityFilter mode, non-authorized applications may access UIB!");
            isUas = true;
        }
        log.trace("Header appCred: {}",applicationCredentialXmlEncoded);
        if (applicationCredentialXmlEncoded != null && !applicationCredentialXmlEncoded.isEmpty()) {
            String applicationCredentialXml = "";
            if (applicationCredentialXmlEncoded != null) {
                applicationCredentialXml = URLDecoder.decode(applicationCredentialXmlEncoded, "UTF-8");
                log.trace("Encoded appCred:" + applicationCredentialXmlEncoded);
            }
            isUas = requestViaUas(applicationCredentialXml);
            log.trace("Request via UAS {}", isUas);
            if (!isUas) {
                notifyFailedAttempt(servletRequest);
            }
        } else {
            notifyFailedAnonymousAttempt(servletRequest);
        }


        String pathInfo = servletRequest.getPathInfo();

        if (isUas) {
            Integer statusCode = authenticateAndAuthorizeRequest(pathInfo);
            if (statusCode == null) {
                chain.doFilter(request, response);
            } else {
                ((HttpServletResponse) response).setStatus(statusCode);
            }
        } else {
            if (isHealthPath(pathInfo)) {
                chain.doFilter(request, response);
            } else {
            	log.warn("FORBIDDEN b/c isUas={}, pathInfo={}", String.valueOf(isUas), pathInfo);
                ((HttpServletResponse) response).setStatus(Response.SC_FORBIDDEN);
            }
        }
    }

    protected boolean isHealthPath(String pathInfo) {
        boolean isHealthPath = false;
        if (pathInfo != null) {
            String path = pathInfo.substring(1);
            if (path != null && path.startsWith(HEALT_PATH)) {
                isHealthPath = true;
            }
        }
        return isHealthPath;
    }

    protected void notifyFailedAnonymousAttempt(HttpServletRequest servletRequest) {
        log.trace("Failed intrusion detected. Header is missing.{}", servletRequest.toString());
        healthCheckService.addIntrusionAnonymous();
    }

    protected void notifyFailedAttempt(HttpServletRequest request) {
        log.trace("Failed intrusion detected {}", request.toString());
        healthCheckService.addIntrusion();
    }

    /*
    Read the credentialXml, and validate this content towards the credentials stored in applicationdatabase.
     */
    protected boolean requestViaUas(String applicationCredentialXml) {
        boolean isUAS = false;
        if (isCI) {
            isUAS = true;
        }
        if (applicationCredentialXml != null && !applicationCredentialXml.isEmpty()) {
            ApplicationCredential applicationCredential = ApplicationCredentialMapper.fromXml(applicationCredentialXml);
            isUAS = authenticationService.isAuthenticatedAsUAS(applicationCredential);
        }
        return isUAS;
    }


    public static void setCIFlag(boolean flag) {
        isCI = flag;

    }

    @Override
    public void destroy() {
    }
}
