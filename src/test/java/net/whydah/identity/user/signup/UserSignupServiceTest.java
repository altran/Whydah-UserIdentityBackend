package net.whydah.identity.user.signup;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.resource.UIBUserAggregateRepresentation;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by baardl on 01.10.15.
 */
public class UserSignupServiceTest  {
    ObjectMapper objectMapper = new ObjectMapper();
    UserPropertyAndRole defaultRole = null;
    String userId = "testId1";

    @BeforeMethod
    public void setUp() throws Exception {
        defaultRole = new UserPropertyAndRole();
        defaultRole.setUid(userId);
        defaultRole.setOrganizationName("whydah");
        defaultRole.setApplicationName("default");
        defaultRole.setApplicationRoleName("whydah-user");
        defaultRole.setApplicationRoleValue("enabled");

    }

    @Test
    public void testCreateUserWithRoles() throws Exception {
        UIBUserAggregateRepresentation userAggregateRepresentation = objectMapper.readValue(userWithoutRolesJson, UIBUserAggregateRepresentation.class);
        UIBUserAggregate userAggregate = userAggregateRepresentation.buildUserAggregate();
        userAggregate.setUid(userId);
        userAggregate.addRole(defaultRole);
        UIBUserAggregateRepresentation userRepWithRole = UIBUserAggregateRepresentation.fromUserAggregate(userAggregate);
        String userWithRole = objectMapper.writeValueAsString(userRepWithRole);
        assertNotNull(userWithRole);
        assertTrue(userWithRole.contains("whydah-user"));
    }

    private String userWithoutRolesJson = "{\"username\":\"helloMe\", \"firstName\":\"hello\", \"lastName\":\"me\", \"personRef\":\"\", \"email\":\"hello.me@example.com\", \"cellPhone\":\"+47 90221133\"}";
}