package net.whydah.iam.service.dataimport;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.whydah.iam.service.dataimport.RoleMappingImporter;
import net.whydah.iam.service.domain.UserPropertyAndRole;

import org.junit.Test;

public class RoleMappingImporterTest {

	@Test
	public void parseRoles() {
		String roleMappingSource = "testrolemappings.csv";
		
		List<UserPropertyAndRole> roleMappings = RoleMappingImporter.parseRoleMapping(roleMappingSource);
		
//		#userId, applicationId, applicationName, organizationId, organizationName, roleName, roleValue
//		thomas.pringle@altran.com, 42, mobilefirst, 23, altran, developer, 30 
//		thomas.pringle@altran.com, 42, mobilefirst, 23, altran, client, 10
//		thomas.pringle@altran.com, 11, whydah, 7, whydah, developer, 20
//		erik.drolshammer, 42, mobilefirst, 23, altran, admin, 70

		assertEquals("All rolemappings must be found.", 4, roleMappings.size());
		
		UserPropertyAndRole roleMapping1 = roleMappings.get(0);
		assertEquals("UserId must be set.", "thomas.pringle@altran.com", roleMapping1.getUid());
		assertEquals("applicationId must be set.", "2", roleMapping1.getAppId());
		assertEquals("applicationName must be set.", "Mobilefirst", roleMapping1.getApplicationName());
		assertEquals("organizationId must be set.", "5", roleMapping1.getOrgId());
		assertEquals("organizationName must be set.", "Altran", roleMapping1.getOrganizationName());
		assertEquals("roleName must be set.", "developer", roleMapping1.getRoleName());
		assertEquals("roleValue must be set.", "30", roleMapping1.getRoleValue());
		
		UserPropertyAndRole roleMapping4 = roleMappings.get(3);
		assertEquals("UserId must be set.", "erik.drolshammer", roleMapping4.getUid());
		assertEquals("applicationId must be set.", "2", roleMapping4.getAppId());
		assertEquals("applicationName must be set.", "Mobilefirst", roleMapping4.getApplicationName());
		assertEquals("organizationId must be set.", "5", roleMapping4.getOrgId());
		assertEquals("organizationName must be set.", "Altran", roleMapping4.getOrganizationName());
		assertEquals("roleName must be set.", "admin", roleMapping4.getRoleName());
		assertEquals("roleValue must be set.", "70", roleMapping4.getRoleValue());
		

	}
}
