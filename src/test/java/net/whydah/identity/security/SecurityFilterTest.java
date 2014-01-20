package net.whydah.identity.security;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;

public class SecurityFilterTest {
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
        when(filterConfig.getInitParameter(SecurityFilter.SECURED_PATHS_PARAM)).thenReturn("/admin,/secured");
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

    private final static String tokenOther = "<application ID=\"1\"><organization ID=\"2\"><role name=\"Vaktmester\"/></organization></application>";
    private final static String tokenBrukeradmin = "<application ID=\"1\"><organization ID=\"2\"><role name=\"WhydahUserAdmin\"/></organization></application>";
}
