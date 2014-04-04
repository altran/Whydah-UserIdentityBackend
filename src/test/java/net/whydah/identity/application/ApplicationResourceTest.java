package net.whydah.identity.application;

import net.whydah.identity.audit.AuditLogRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
public class ApplicationResourceTest {
    private static final Logger log = LoggerFactory.getLogger(ApplicationResourceTest.class);
    ApplicationRepository applicationRepositoryMock;
    AuditLogRepository auditLogRepositoryMock;
    ApplicationService applicationService;
    ApplicationResource applicationResource;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        applicationRepositoryMock = mock(ApplicationRepository.class);
        auditLogRepositoryMock = mock(AuditLogRepository.class);

        applicationService = new ApplicationService(applicationRepositoryMock, auditLogRepositoryMock);
        applicationResource = new ApplicationResource(applicationService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateApplication() throws Exception {
        applicationResource.createApplication(allApplication);
    }

    public void testCreateApplicationFails() throws Exception {
        applicationResource.createApplication("malformedjson");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
    private final String allApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRole\":\"default1role\",\"defaultOrgid\":\"defaultorgid\",\"availableOrgIds\":[\"developer@customer\",\"consultant@customer\"]}";
}
