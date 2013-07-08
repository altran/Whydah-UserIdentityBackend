package no.freecode.iam.service.user;

import static org.junit.Assert.assertEquals;

import java.util.List;

import no.freecode.iam.service.domain.UserPropertyAndRole;

import org.junit.Test;

public class RoleMappingImporterTest {

	@Test
	public void parseRoles() {
		String roleMappingSource = "testrolemapping.csv";
		
		List<UserPropertyAndRole> roleMappings = WhydahRoleMappingImporter.parseRoleMapping(roleMappingSource);
		
//		#userId, applicationId, applicationName, organizationId, organizationName, roleName, roleValue
//		thomas.pringle@altran.com, 42, mobilefirst, 23, altran, developer, 30 
//		thomas.pringle@altran.com, 42, mobilefirst, 23, altran, client, 10
//		thomas.pringle@altran.com, 11, whydah, 7, whydah, developer, 20
//		erik.drolshammer, 42, mobilefirst, 23, altran, admin, 70

		assertEquals("All rolemappings must be found.", 4, roleMappings.size());
		
		UserPropertyAndRole roleMapping1 = roleMappings.get(0);
		assertEquals("UserId must be set.", "thomas.pringle@altran.com", roleMapping1.getUid());
		assertEquals("applicationId must be set.", "42", roleMapping1.getAppId());
		assertEquals("applicationName must be set.", "mobilefirst", roleMapping1.getApplicationName());
		assertEquals("organizationId must be set.", "23", roleMapping1.getOrgId());
		assertEquals("organizationName must be set.", "altran", roleMapping1.getOrganizationName());
		assertEquals("roleName must be set.", "developer", roleMapping1.getRoleName());
		assertEquals("roleValue must be set.", "30", roleMapping1.getRoleValue());
		
		UserPropertyAndRole roleMapping4 = roleMappings.get(3);
		assertEquals("UserId must be set.", "erik.drolshammer", roleMapping4.getUid());
		assertEquals("applicationId must be set.", "42", roleMapping4.getAppId());
		assertEquals("applicationName must be set.", "mobilefirst", roleMapping4.getApplicationName());
		assertEquals("organizationId must be set.", "23", roleMapping4.getOrgId());
		assertEquals("organizationName must be set.", "altran", roleMapping4.getOrganizationName());
		assertEquals("roleName must be set.", "admin", roleMapping4.getRoleName());
		assertEquals("roleValue must be set.", "70", roleMapping4.getRoleValue());
		

	}
}
