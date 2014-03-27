package net.whydah.identity.security;

import net.whydah.identity.user.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sjekker om request path krever autentisering, og i såfall sjekkes usertoken.
 * Secured paths are added as comma separated list in filterConfig. Required role is also configured with filterConfig.
 */
public class SecurityFilter implements Filter {
    public static final String SECURED_PATHS_PARAM = "securedPaths";
    public static final String REQUIRED_ROLE_PARAM = "requiredRole";
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final SecurityTokenHelper securityTokenHelper;
    private List<String> securedPaths = new ArrayList<>();
    private String requiredRole;

    public SecurityFilter(SecurityTokenHelper securityTokenHelper) {
        this.securityTokenHelper = securityTokenHelper;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String securedPathsParam = filterConfig.getInitParameter(SECURED_PATHS_PARAM);
        if (securedPathsParam != null) {
            securedPaths = Arrays.asList(securedPathsParam.split(","));
        }
        requiredRole = filterConfig.getInitParameter(REQUIRED_ROLE_PARAM);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String pathInfo = servletRequest.getPathInfo();
        if (isSecuredPath(pathInfo)) {
            String usertokenId = getTokenIDFromPath(pathInfo);
            logger.debug("usertokenid: {} from path={} ", usertokenId, pathInfo);
            if (usertokenId == null) {
                logger.info("Token not found");
                setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            UserToken userToken = securityTokenHelper.getUserToken(usertokenId);

            if (userToken == null) {
                logger.info("Could not find token with tokenID=" + usertokenId);
                setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (!userToken.hasRole(requiredRole)) {
                logger.info("Missing required role {}", requiredRole);
                setResponseStatus((HttpServletResponse) response, HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            logger.debug("setAuthenticatedUser with usertoken: {}", userToken);
            Authentication.setAuthenticatedUser(userToken);
        }
        chain.doFilter(request, response);
        Authentication.clearAuthentication();
    }

    private void setResponseStatus(HttpServletResponse response, int statuscode) {
        response.setStatus(statuscode);
    }

    /**
     * Plukker element 2 fra path som usertokenid. F.eks. /useradmin/1kj2h1j12jh/users/add gir 1kj2h1j12jh.
     * @param pathInfo fra servletRequest.getPathInfo()
     * @return usertoken
     */
    private String getTokenIDFromPath(String pathInfo) {
        int start = pathInfo.indexOf('/', 1);
        if(start < 0) {
            return null;
        }
        int stop = pathInfo.indexOf('/', start + 1);
        if(stop < 0) {
            return null;
        }
        return pathInfo.substring(start + 1, stop);
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
