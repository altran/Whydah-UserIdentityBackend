package net.whydah.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.audit.AuditLogDao;
import org.junit.Before;

import static org.mockito.Mockito.mock;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
public class ApplicationServiceTest {
    ApplicationDao applicationDaoMock;
    AuditLogDao auditLogDaoMock;
    ApplicationService applicationService;
    ObjectMapper mapper = new ObjectMapper();

    private final String allApplication = "{\"id\":\"11\",\"name\":\"SecurityTokenService\",\"defaultRoleName\":\"WhydahDefaultUser\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";
    private final String application2 = "{\"id\":\"12\",\"name\":\"UserAdminService\",\"defaultRoleName\":\"WhydahUserAdmin\",\"defaultOrgName\":\"Whydah\",\"availableOrgNames\":[\"Whydah\",\"ACSOrganization\"],\"availableRoleNames\":[\"WhydahDefaultUser\",\"WhydahUserAdmin\"]}";


    //private final String minimumApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":null,\"defaultOrgName\":null,\"availableOrgNames\":[]}";
    //private final String mostApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":\"defaultrole\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[]}";
    //private final String allApplication = "{\"id\":\"id1\",\"name\":\"test\",\"defaultRoleName\":\"defaultrole\",\"defaultOrgName\":\"defaultorgname\",\"availableOrgNames\":[\"developer@customer\",\"consultant@customer\"]}";

    private static final String minApplication = "{\"id\":\"id1\",\"name\":\"name1\",\"description\":null,\"availableRoles\":[],\"defaultRoleName\":null,\"availableOrgNames\":[],\"defaultOrgName\":null,\"availableRoleNames\":null}";
    private static final String maxApplication = "{\"id\":\"appId1\",\"name\":\"appName1\",\"description\":\"description1\",\"availableRoles\":[{\"id\":\"roleId1\",\"name\":\"roleName1\"},{\"id\":\"roleId2\",\"name\":\"roleName2\"}],\"defaultRoleName\":\"defaultRoleName1\",\"availableOrgNames\":[\"orgName1\",\"orgName2\",\"orgName3\"],\"defaultOrgName\":\"defaultOrgName1\",\"availableRoleNames\":[\"roleName1\",\"roleName2\"]}";


    @Before
    public void setUp() throws Exception {
        applicationDaoMock = mock(ApplicationDao.class);
        auditLogDaoMock = mock(AuditLogDao.class);
        applicationService = new ApplicationService(applicationDaoMock, auditLogDaoMock);
    }

    /*
    @Test
    public void testCreateApplication() throws Exception {
        Application application = applicationService.create(allApplication);
        assertNotNull(application);
        assertEquals("11", application.getId());
        assertEquals("SecurityTokenService", application.getName());
        assertEquals("WhydahDefaultUser", application.getDefaultRoleName());
        assertEquals("Whydah", application.getDefaultOrgName());
        List<String> availableOrgNames = application.getAvailableOrgNames();
        assertNotNull(availableOrgNames);
        assertEquals("Whydah", availableOrgNames.get(0));
        assertEquals("ACSOrganization", availableOrgNames.get(1));
        Application aapplication = applicationService.create(allApplication);

    }

    @Test
    public void testCreateDuplicateApplication() throws Exception {
        Application application = applicationService.create(allApplication);
        applicationService.create(allApplication);
        applicationService.create(allApplication);
        applicationService.create(allApplication);
    }

    @Test
    public void testGetApplications() throws Exception {
        Application application = applicationService.create(allApplication);
        Application application_2 = applicationService.create(application2);
        List<Application> applications = new LinkedList();
        applications.add(application);
        applications.add(application_2);
        assertNotNull(applications);
        assertEquals(2,applications.size());
    }
    */
}
