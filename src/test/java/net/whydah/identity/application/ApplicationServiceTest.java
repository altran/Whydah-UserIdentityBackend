package net.whydah.identity.application;

import net.whydah.identity.audit.AuditLogRepository;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
public class ApplicationServiceTest {
    ApplicationRepository applicationRepositoryMock;
    AuditLogRepository auditLogRepositoryMock;
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        applicationRepositoryMock = mock(ApplicationRepository.class);
        auditLogRepositoryMock = mock(AuditLogRepository.class);
        applicationService = new ApplicationService(applicationRepositoryMock, auditLogRepositoryMock);

    }
    @Test
    public void testCreateApplication() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        assertNotNull(application);
        assertEquals("11", application.getId());
        assertEquals("test", application.getName());
        assertEquals("default1role", application.getDefaultRoleName());
        assertEquals("defaultorgname", application.getDefaultOrgName());
        List<String> availableOrgNames = application.getAvailableOrgNames();
        assertNotNull(availableOrgNames);
        assertEquals("developer@customer", availableOrgNames.get(0));
        assertEquals("consultant@customer", availableOrgNames.get(1));

    }
    @Test
    public void testGetApplications() throws Exception {
        Application application = applicationService.createApplication(allApplication);
        Application application_2 = applicationService.createApplication(application2);
        List<Application> applications = new LinkedList();
        applications.add(application);
        applications.add(application_2);
        assertNotNull(applications);
        assertEquals(2,applications.size());

        System.out.println(buildApplicationsJson(applications));
    }

    private final String allApplication = "{\"id\":\"11\",\"name\":\"SecurityTokenService\",\"defaultRoleName\":\"WhydahDefaultUser\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";
    private final String application2 = "{\"id\":\"12\",\"name\":\"UserAdminService\",\"defaultRoleName\":\"WhydahUserAdmin\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";

    protected String buildApplicationsJson(List<Application> applications) {
        String applicationsCreatedJson = null;
        try {
            applicationsCreatedJson = mapper.writeValueAsString(applications);
        } catch (IOException e) {
        }
        return applicationsCreatedJson;
    }
}
