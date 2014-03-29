package net.whydah.identity.security;

import net.whydah.identity.applicationtoken.ApplicationTokenService;
import net.whydah.identity.usertoken.SecurityTokenHelper;
import net.whydah.identity.usertoken.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes usertoken.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
public class SecurityFilter implements Filter {
    public static final String OPEN_PATH = "/applicationtoken";
    public static final String USER_TOKEN_PATH = "/usertoken";
    public static final String SECURED_PATHS_PARAM = "securedPaths";
    public static final String REQUIRED_ROLE_USERS = "WhydahUserAdmin";
    public static final String REQUIRED_ROLE_APPLICATIONS = "WhydahUserAdmin";
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final SecurityTokenHelper securityTokenHelper;
    private final ApplicationTokenService applicationTokenService;
    private List<String> securedPaths = new ArrayList<>();
    private String requiredRole;

    public SecurityFilter(SecurityTokenHelper securityTokenHelper, ApplicationTokenService applicationTokenService) {
        this.securityTokenHelper = securityTokenHelper;
        this.applicationTokenService = applicationTokenService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        requiredRole = REQUIRED_ROLE_USERS;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String pathInfo = servletRequest.getPathInfo();
        if (isOpenPath(pathInfo)) {
            chain.doFilter(request, response);
        } else {
            if (isUserTokenPath(pathInfo)) {
                //Verify applicationTokenId
                String applicationTokenId = findPathElement(pathInfo, 1);
                if (applicationTokenService.verifyApplication(applicationTokenId)) {
                    chain.doFilter(request,response);
                } else {
                    logger.trace("Application not Authorized=" + applicationTokenId);
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else {
                //Verify userTokenId
                String usertokenId = findUserTokenId(pathInfo);
                logger.debug("usertokenid: {} from path={} ", usertokenId, pathInfo);
                if (usertokenId == null) {
                    logger.trace("Token not found");
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                UserToken userToken = securityTokenHelper.getUserToken(usertokenId);

                if (userToken == null) {
                    logger.trace("Could not find token with tokenID=" + usertokenId);
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                if (!userToken.hasRole(requiredRole)) {
                    logger.trace("Missing required role {}", requiredRole);
                    setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                logger.debug("setAuthenticatedUser with usertoken: {}", userToken);
                Authentication.setAuthenticatedUser(userToken);
                chain.doFilter(request, response);
            }

        }
        Authentication.clearAuthentication();
    }

    private boolean isUserTokenPath(String pathInfo) {
        boolean isUserTokenPath = false;
        String pathElement = findPathElement(pathInfo,2);
        if (pathElement != null) {
            isUserTokenPath = pathElement.startsWith(USER_TOKEN_PATH);
        }
        return isUserTokenPath;
    }

    protected String findPathElement(String pathInfo, int elementNumber) {
        String pathElement = null;
        if (pathInfo != null) {
            String[] pathElements = pathInfo.split("/");
            if (pathElements.length > elementNumber) {
                pathElement = "/" +pathElements[elementNumber];
            }
        }
        return pathElement;
    }

    protected boolean isOpenPath(String pathInfo) {
        return pathInfo.startsWith(OPEN_PATH);
    }

    private void setResponseStatus(HttpServletResponse response, int statuscode) {
        response.setStatus(statuscode);
    }

    /**
     * Plukker element 2 fra path som usertokenid. F.eks. /useradmin/1kj2h1j12jh/users/add gir 1kj2h1j12jh.
     * @param pathInfo fra servletRequest.getPathInfo()
     * @return usertoken
     */
    protected String findUserTokenId(String pathInfo) {
        String tokenIdPath = findPathElement(pathInfo, 1);
        String tokenId = null;
        if (tokenIdPath != null) {
            tokenId = tokenIdPath.substring(1);
        }
        return tokenId;

    }

    private boolean isSecuredPath(String pathInfo) {
        for (String securedPath : securedPaths) {
            if (pathInfo.startsWith(securedPath)) {
                logger.info("Secured: {}", pathInfo);
                return true;
            }
        }
        logger.info("Not secured: {}", pathInfo);
        return false;
    }

    @Override
    public void destroy() {
    }
}
