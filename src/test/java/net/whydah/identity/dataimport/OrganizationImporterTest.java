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
        assertAppIdAndOrgName(organizations.get(0), "2001", "Whydah");
        assertAppIdAndOrgName(organizations.get(1), "2011", "Cantara");
        assertAppIdAndOrgName(organizations.get(2), "2012", "Whydah");
        assertAppIdAndOrgName(organizations.get(3), "2022", "Cantara");
        assertAppIdAndOrgName(organizations.get(4), "2013", "Whydah");
        assertAppIdAndOrgName(organizations.get(5), "2023", "Cantara");
    }

    private void assertAppIdAndOrgName(Organization organization, String appId, String orgName) {
        assertEquals("applicationId must be set.", appId, organization.getAppId());
        assertEquals("organizationName must be set.", orgName, organization.getName());
    }
}
