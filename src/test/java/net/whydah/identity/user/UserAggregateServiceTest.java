package net.whydah.identity.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.resource.UIBUserAggregateRepresentation;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 06/04/14
 */
@Ignore
public class UserAggregateServiceTest {
    private static final Logger log = LoggerFactory.getLogger(UserAggregateServiceTest.class);
    /*
{
  "uid": "uid",
  "username": "usernameABC",
  "firstName": "firstName",
  "lastName": "lastName",
  "personRef": "personRef",
  "email": "email",
  "cellPhone": "12345678",
  "password": "password",
  "roles": [
    {
      "applicationId": "applicationId",
      "applicationName": "applicationName",
      "organizationId": "organizationId",
      "organizationName": "organizationName",
      "applicationRoleName": "roleName",
      "applicationRoleValue": "email"
    },
    {
      "applicationId": "applicationId123",
      "applicationName": "applicationName123",
      "organizationId": "organizationId123",
      "organizationName": "organizationName123",
      "applicationRoleName": "roleName123",
      "applicationRoleValue": "roleValue123"
    }
  ]
}
     */


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

    @BeforeClass
    public static void stap() {
        FileUtils.deleteDirectory(new File("target/data/lucene"));
        FileUtils.deleteDirectory(new File("data/lucene"));

    }

    @AfterClass
    public static void stop() {
        FileUtils.deleteDirectory(new File("target/data/"));

    }

    @Test   //ED: In-progress
    @Ignore
    public void testJsonFromUserAggregate() throws IOException {
        String username = "usernameABC";
        LDAPUserIdentity userIdentity = new LDAPUserIdentity("uid", username, "firstName", "lastName", "email", "password", "12345678", "personRef"
        );

        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(userIdentity.getUid());
        role.setApplicationId("applicationId");
        role.setApplicationName("applicationName");
        role.setOrganizationName("organizationName");
        role.setApplicationRoleName("roleName");
        //role.setRoleValue(roleValue);
        role.setApplicationRoleValue(userIdentity.getEmail());  // Provide NetIQ identity as rolevalue

        UserPropertyAndRole role2 = new UserPropertyAndRole();
        role2.setUid(userIdentity.getUid());
        role2.setApplicationId("applicationId123");
        role2.setApplicationName("applicationName123");
        role2.setOrganizationName("organizationName123");
        role2.setApplicationRoleName("roleName123");
        //role.setRoleValue(roleValue);
        role2.setApplicationRoleValue("roleValue123");

        List<UserPropertyAndRole> roles = new ArrayList<>(2);
        roles.add(role);
        roles.add(role2);
        UIBUserAggregate userAggregate = new UIBUserAggregate(userIdentity, roles);

        UIBUserAggregateRepresentation userRepresentation = UIBUserAggregateRepresentation.fromUserAggregate(userAggregate);

        ObjectMapper objectMapper = new ObjectMapper();
        Writer strWriter = new StringWriter();
        objectMapper.writeValue(strWriter, userRepresentation);
        String json = strWriter.toString();
        assertNotNull(json);
        log.debug("json: {}", json);
    }
}