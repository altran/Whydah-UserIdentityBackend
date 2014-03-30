package net.whydah.identity.security;

import net.whydah.identity.application.authentication.ApplicationTokenService;
import net.whydah.identity.user.authentication.SecurityTokenHelper;
import net.whydah.identity.user.authentication.UserToken;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SecurityFilterTest {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilterTest.class);
    private SecurityFilter securityFilter;
    private SecurityTokenHelper tokenHelper;
    private ApplicationTokenService applicationTokenService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    FilterChain chain;

    @Before
    public void init() throws ServletException {
        tokenHelper = mock(SecurityTokenHelper.class);
        applicationTokenService = mock(ApplicationTokenService.class);

        securityFilter = new SecurityFilter(tokenHelper, applicationTokenService);

        securityFilter.init(null);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    public void testSecuredTokenNotValid() throws Exception {
        when(request.getPathInfo()).thenReturn("/" +userTokenInvalid +"/users");
        when(tokenHelper.getUserToken("any",userTokenInvalid)).thenReturn(null);
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenOkMissingGroup() throws Exception {
        when(request.getPathInfo()).thenReturn("/" +userTokenMissingGroup +"/users");
        when(tokenHelper.getUserToken("any",userTokenMissingGroup)).thenReturn(new UserToken(tokenOther));
        securityFilter.doFilter(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testSecuredTokenOkRoleOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/" + userAdminUserTokenId +"/users");
        when(tokenHelper.getUserToken("any",userAdminUserTokenId)).thenReturn(new UserToken(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void userOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/" + userAdminUserTokenId +"/user");
        when(tokenHelper.getUserToken("any",userAdminUserTokenId)).thenReturn(new UserToken(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
    @Test
    public void applicationOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/" + userAdminUserTokenId +"/application");
        when(tokenHelper.getUserToken("any",userAdminUserTokenId)).thenReturn(new UserToken(tokenBrukeradmin));
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
    @Test
    public void applicationsOk() throws Exception {
        when(request.getPathInfo()).thenReturn("/" + userAdminUserTokenId +"/applications");
        when(tokenHelper.getUserToken("any",userAdminUserTokenId)).thenReturn(new UserToken(tokenBrukeradmin));
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
    public void findUserTokenId() throws Exception {
        assertEquals(null, securityFilter.findUserTokenId(null));
        assertEquals("1234", securityFilter.findUserTokenId("/1234"));

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
        when(request.getPathInfo()).thenReturn("/" + applicationTokenId + "/usertoken/");
        when(applicationTokenService.verifyApplication(anyString())).thenReturn(true);
        securityFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        log.debug("Status {}", response.getStatus());
    }
    @Test
    public void verifyUserTokenUrlMissingApplicationTokenId() throws Exception {
        when(request.getPathInfo()).thenReturn("//usertoken/");
        securityFilter.doFilter(request, response,chain);
        log.debug("Status {}", response.getStatus());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }
    @Test
    public void verifyUserTokenUrlWrongUrl() throws Exception {
        when(request.getPathInfo()).thenReturn("/usertoken/");
        securityFilter.doFilter(request, response,chain);
        log.debug("Status {}", response.getStatus());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(chain);
    }

    private final static String tokenOther = "<application ID=\"1\"><organization ID=\"2\"><role name=\"Vaktmester\"/></organization></application>";
    private final static String tokenBrukeradmin = "<application ID=\"1\"><organization ID=\"2\"><role name=\"WhydahUserAdmin\"/></organization></application>";
    private final static String applicationToken = "<application ID=\"abcdefgid\"></application>";
    private final static String applicationTokenId="abcdefgid";
    private final static String userAdminUserTokenId ="au123";
    private final static String userTokenInvalid ="uti123";
    private final static String userTokenMissingGroup ="utig123";

}