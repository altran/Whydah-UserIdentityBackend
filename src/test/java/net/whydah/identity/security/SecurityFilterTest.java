package net.whydah.identity.security;

import net.whydah.identity.usertoken.SecurityTokenHelper;
import net.whydah.identity.usertoken.UserToken;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SecurityFilterTest {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilterTest.class);
    private static final String CONTEXT_PATH = "/uib";
    private SecurityFilter securityFilter;
    private SecurityTokenHelper tokenHelper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    FilterChain chain;

    @Before
    public void init() throws ServletException {
        tokenHelper = mock(SecurityTokenHelper.class);
        securityFilter = new SecurityFilter(tokenHelper);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter(SecurityFilter.SECURED_PATHS_PARAM)).thenReturn("/admin,/secured,/uib");
        when(filterConfig.getInitParameter(SecurityFilter.REQUIRED_ROLE_PARAM)).thenReturn("WhydahUserAdmin");
        securityFilter.init(filterConfig);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    public void testNotSecured() throws Exception {
        when(request.getPathInfo()).thenReturn("/get");
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testSecuredNoTokenid() throws Exception {
        when(request.getPathInfo()).thenReturn("/admin/");
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenNotValid() throws Exception {
        when(request.getPathInfo()).thenReturn("/admin/thetoken/users");
        when(tokenHelper.getUserToken("thetoken")).thenReturn(null);
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenOkMissingGroup() throws Exception {
        when(request.getPathInfo()).thenReturn("/admin/thetoken/users");
        when(tokenHelper.getUserToken("thetoken")).thenReturn(new UserToken(tokenOther));
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenOkRoleOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/admin/thetoken/users");
        when(tokenHelper.getUserToken("thetoken")).thenReturn(new UserToken(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void findPathElement() throws Exception {
        assertEquals("/usertoken", securityFilter.findPathElement("/123/usertoken/", 2));
        assertEquals("/123", securityFilter.findPathElement("/123/usertoken/", 1));
        assertEquals(null, securityFilter.findPathElement("/123/usertoken/", 3));
        assertEquals(null, securityFilter.findPathElement("", 3));
        assertEquals(null, securityFilter.findPathElement(null, 1));
    }

    @Test
    public void verifyApplicationTokenUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/applicationtoken/");
        securityFilter.doFilter(request, response,chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
    }

    @Test
    public void verifyUserTokenUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/" + applicationTokenId +"/usertoken/");
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
    }
    @Test
    public void verifyUserTokenUrlMissingApplicationTokenId() throws Exception {
        when(request.getPathInfo()).thenReturn(CONTEXT_PATH +"/usertoken/");
        securityFilter.doFilter(request, response,chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private final static String tokenOther = "<application ID=\"1\"><organization ID=\"2\"><role name=\"Vaktmester\"/></organization></application>";
    private final static String tokenBrukeradmin = "<application ID=\"1\"><organization ID=\"2\"><role name=\"WhydahUserAdmin\"/></organization></application>";
    private final static String applicationToken = "<application ID=\"abcdefgid\"></application>";
    private final static String applicationTokenId="abcdefgid";
}
