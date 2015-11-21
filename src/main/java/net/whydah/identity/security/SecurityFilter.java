package net.whydah.identity.security;

import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.user.UserRole;
import net.whydah.identity.user.authentication.SecurityTokenServiceHelper;
import net.whydah.identity.user.authentication.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes authentication.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
@Component
public class SecurityFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private final SecurityTokenServiceHelper securityTokenHelper;

    //public static final String OPEN_PATH = "/authenticate";
    //public static final String AUTHENTICATE_USER_PATH = "/authenticate";
    //public static final String PASSWORD_RESET_PATH = "/password";
    //public static final String SECURED_PATHS_PARAM = "securedPaths";
    //public static final String REQUIRED_ROLE_USERS = "WhydahUserAdmin";
    //public static final String REQUIRED_ROLE_APPLICATIONS = "WhydahUserAdmin";
    //private List<String> securedPaths = new ArrayList<>();
    //private String requiredRole;

    @Autowired
    public SecurityFilter(SecurityTokenServiceHelper securityTokenHelper) {
        this.securityTokenHelper = securityTokenHelper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //requiredRole = REQUIRED_ROLE_USERS;
    }


    /**
     *
     * @param pathInfo  the path to apply the filter to
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
        if (path.startsWith("health")) {
            return null;
        }

        if (ApplicationMode.skipSecurityFilter()) {
            log.warn("Running in noSecurityFilter mode, security is omitted for users.");
            Authentication.setAuthenticatedUser(buildMockedUserToken());
            return null;
        }


        // TODO fetch UAS applicationTokenId from http header, call CommandValidateApplicationTokenId
        // /{stsApplicationtokenId}/application/auth        //ApplicationAuthenticationEndpoint


        //strip applicationTokenId from pathInfo
        path = path.substring(path.indexOf("/"));


        //paths without userTokenId verification
        /*
        /{applicationTokenId}/user/{uid}/reset_password     //PasswordResource2
        /{applicationTokenId}/user/{uid}/change_password    //PasswordResource2
        /{applicationTokenId}/authenticate/user/*           //UserAuthenticationEndpoint
        /{applicationTokenId}/signup/user                   //UserSignupEndpoint
        */
        String pwPattern = "/user/.+/(reset|change)_password";
        String userAuthPattern = "/authenticate/user(|/.*)";
        String userSignupPattern = "/signup/user";
        String [] patternsWithoutUserTokenId = {pwPattern, userAuthPattern, userSignupPattern};
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
        UserToken userToken = securityTokenHelper.getUserToken(applicationTokenId, usertokenId);
        if (userToken == null || userToken.toString().length() < 10) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }

        //TODO verify required user role

        Authentication.setAuthenticatedUser(userToken);
        return null;
    }

    UserToken buildMockedUserToken() {
        List<UserRole> roles = new ArrayList<>();
        roles.add(new UserRole("9999", "99999", "mockrole"));
        return new UserToken("MockUserToken", roles);
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

        Integer statusCode = authenticateAndAuthorizeRequest(servletRequest.getPathInfo());
        if (statusCode == null) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(statusCode);
        }
    }


    @Override
    public void destroy() {
    }
}
