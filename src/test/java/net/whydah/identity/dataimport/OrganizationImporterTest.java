package net.whydah.identity.dataimport;

import net.whydah.identity.util.FileUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class OrganizationImporterTest {

	@Test
	public void parseRoles() {
		String organizationsSource = "testorganizations.csv";
//      #applicationId, organizationName
//      1, Whydah
//      1, Cantara
//      2, Whydah
//      2, Cantara
//      3, Whydah
//      3, Cantara

        InputStream organizationsStream = FileUtils.openFileOnClasspath(organizationsSource);
        List<Organization> organizations = OrganizationImporter.parseOrganizations(organizationsStream);
		
        assertEquals("All organizations must be found.", 6, organizations.size());
        assertAppIdAndOrgName(organizations.get(0), "1", "Whydah");
        assertAppIdAndOrgName(organizations.get(1), "1", "Cantara");
        assertAppIdAndOrgName(organizations.get(2), "2", "Whydah");
        assertAppIdAndOrgName(organizations.get(3), "2", "Cantara");
        assertAppIdAndOrgName(organizations.get(4), "3", "Whydah");
        assertAppIdAndOrgName(organizations.get(5), "3", "Cantara");
	}

    private void assertAppIdAndOrgName(Organization organization, String appId, String orgName) {
        assertEquals("applicationId must be set.", appId, organization.getAppId());
        assertEquals("organizationName must be set.", orgName, organization.getName());
    }
}
