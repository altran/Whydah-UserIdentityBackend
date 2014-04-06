package net.whydah.identity.user;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 06/04/14
 */
public class UserAggregateServiceTest {

    /*
    {
  "identity": {
    "uid": "uid",
    "username": "username123",
    "firstName": "firstName",
    "lastName": "lastName",
    "personRef": "personRef",
    "email": "email",
    "cellPhone": "12345678",
    "personName": "firstName lastName",
    "password": "password"
  },
  "userPropertiesAndRolesList": [
    {
      "uid": "uid",
      "applicationId": "applicationId",
      "orgId": "organizationId",
      "applicationRoleName": "roleName",
      "applicationRoleValue": "email",
      "organizationName": "organizationName",
      "applicationName": "applicationName"
    }
  ]
}
     */

    @Test   //ED: In-progress
    public void testJsonFromUserAggregate() throws IOException {
        String username = "username123";
        UserIdentity userIdentity = new UserIdentity("uid", username, "firstName", "lastName", "personRef", "email",
                "12345678", "password");

        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(userIdentity.getUid());
        role.setApplicationId("applicationId");
        role.setApplicationName("applicationName");
        role.setOrgId("organizationId");
        role.setOrganizationName("organizationName");
        role.setApplicationRoleName("roleName");
        //role.setRoleValue(roleValue);
        role.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue

        List<UserPropertyAndRole> roles = new ArrayList<>(1);
        roles.add(role);
        UserAggregate userAggregate = new UserAggregate(userIdentity, roles);


        ObjectMapper objectMapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        objectMapper.writeValue(strWriter, userAggregate);
        String json = strWriter.toString();
        assertNotNull(json);
    }
}