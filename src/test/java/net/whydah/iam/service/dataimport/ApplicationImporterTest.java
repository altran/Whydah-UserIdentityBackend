package net.whydah.iam.service.dataimport;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.whydah.iam.service.dataimport.Application;
import net.whydah.iam.service.dataimport.ApplicationImporter;

import org.junit.Test;

public class ApplicationImporterTest {

	@Test
	public void parseRoles() {
		String applicationsSource = "testapplications.csv";
		
		List<Application> applications = ApplicationImporter.parseApplications(applicationsSource);
		
//		#applicationId, applicationName, defaultRole, defaultOrgId
//		1, Brukeradmin, Brukeradmin, 1
//		2, Mobilefirst, Client, 2
//		3, Development, Developer, 2
		
		assertEquals("All organizations must be found.", 3, applications.size());
		
		Application application1 = applications.get(0);
		assertEquals("applicationId must be set.", "1", application1.getId());
		assertEquals("applicationName must be set.", "Brukeradmin", application1.getName());
		assertEquals("defaultRole must be set.", "Brukeradmin", application1.getDefaultRoleName());
		assertEquals("defaultOrgId must be set.", "1", application1.getDefaultOrganizationId());
		

		Application application3 = applications.get(2);
		assertEquals("applicationId must be set.", "3", application3.getId());
		assertEquals("applicationName must be set.", "Whydah", application3.getName());
		assertEquals("defaultRole must be set.", "WhydahDefaultUser", application3.getDefaultRoleName());
		assertEquals("defaultOrgId must be set.", "2", application3.getDefaultOrganizationId());

	}
}
