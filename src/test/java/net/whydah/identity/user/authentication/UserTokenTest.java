package net.whydah.identity.user.authentication;

import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UserTokenTest {
    @Test
    public void hasRole() {
        UserToken token = UserTokenMapper.fromUserTokenXml(usertoken);
        assertTrue(hasRole(token,"WhydahUserAdmin"));
        assertFalse(hasRole(token,"nesevis"));
    }
    @Test
    public void getRoles() {
        UserToken token = UserTokenMapper.fromUserTokenXml(usertoken);

        UserApplicationRoleEntry expectedRole1 = new UserApplicationRoleEntry();
        expectedRole1.setId("1");
        expectedRole1.setApplicationName("WHYDAH");
        expectedRole1.setRoleName("WhydahUserAdmin");
        UserApplicationRoleEntry expectedRole2 = new UserApplicationRoleEntry();
        expectedRole2.setId("1");
        expectedRole2.setApplicationName("WHYDAH");
        expectedRole2.setRoleName("Tester");
        UserApplicationRoleEntry expectedRole3 = new UserApplicationRoleEntry();
        expectedRole3.setId("005");
        expectedRole3.setApplicationName("NBBL");
        expectedRole3.setRoleName("Tester");
        List<UserApplicationRoleEntry> actualRoles = token.getRoleList();
        assertNotNull(actualRoles);
        assertEquals(3, actualRoles.size());
        assertTrue(hasRole(token,expectedRole1.getRoleName()));
        assertTrue(hasRole(token,expectedRole2.getRoleName()));
        assertTrue(hasRole(token,expectedRole3.getRoleName()));
    }

    public boolean hasRole(UserToken token,String rolename) {
        for (UserApplicationRoleEntry userRole : token.getRoleList()) {
            if (rolename.equals(userRole.getRoleName())) {
                return true;
            }
        }
        return false;
    }


    private final static String usertoken = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"b035df2e-e766-4077-a514-2c370cc78714\">\n" +
            "    <securitylevel>1</securitylevel>\n" +
            "    <uid>mytestuid</uid>\n" +
            "    <personid></personid>\n" +
            "    <username>testuser</username>\n" +
            "    <firstname>User</firstname>\n" +
            "    <lastname>Admin</lastname>\n" +
            "    <timestamp>" + String.valueOf(System.currentTimeMillis()) + "</timestamp>\n" +
            "    <lifespan>200000</lifespan>\n" +
            "    <issuer>http://10.10.3.88:9998/user/9056ac3f744957ae6a86daffb5aa98d3/usertoken</issuer>\n" +
            "    <application ID=\"145445\">\n" +
            "        <applicationName>WhydahUserAdmin</applicationName>\n" +
            "            <organizationName>WHYDAH</organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"\"/>\n" +
            "            <role name=\"Tester\" value=\"\"/>\n" +

            "    </application>\n" +
            "    <application ID=\"5005\">\n" +
            "        <applicationName>HMS</applicationName>\n" +
            "            <organizationName>NBBL</organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"\"/>\n" +
            "    </application>\n" +
            "\n" +
            "    <ns2:link type=\"application/xml\" href=\"/b035df2e-e766-4077-a514-2c370cc78714\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">6660ae2fcaa0b8311661fa9e3234eb7a</hash>\n" +
            "</usertoken>";

    @Test
    public void getRoles2() {
        UserToken token = UserTokenMapper.fromUserTokenXml(usertoken2);
        UserApplicationRoleEntry expectedRole1 = new UserApplicationRoleEntry();
        expectedRole1.setId("1");
        expectedRole1.setRoleName("WhydahUserAdmin");
        List<UserApplicationRoleEntry> actualRoles = token.getRoleList();
        assertNotNull(actualRoles);
        assertEquals(1, actualRoles.size());
        assertTrue(hasRole(token,expectedRole1.getRoleName()));
    }

    private final static String usertoken2 = "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"12b84a5a-595b-49df-bb20-26a8a974d7b9\">\n" +
            "    <uid>useradmin</uid>\n" +
            "    <username>testusername</username>\n" +
            "    <timestamp>" + String.valueOf(System.currentTimeMillis()) + "</timestamp>\n" +
            "    <lifespan>3600000</lifespan>\n" +
            "    <issuer>http://localhost:9998/tokenservice/user/e0287c65a5c9300c476b34edd0446778/get_usertoken_by_usertokenid</issuer>\n" +
            "    <securitylevel>1</securitylevel>\n" +
            "    <username>admin</username>\n" +
            "    <firstname>User</firstname>\n" +
            "    <lastname>Admin</lastname>\n" +
            "    <email>useradmin@altran.com</email>\n" +
            "    <personRef>0</personRef>\n" +
            "    <application ID=\"45421\">\n" +
            "        <applicationName>UserAdmin</applicationName>\n" +
            "            <organizationName></organizationName>\n" +
            "            <role name=\"WhydahUserAdmin\" value=\"99\"/>\n" +
            "    </application>\n" +
            "\n" +
            "    <ns2:link type=\"application/xml\" href=\"/\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">9fc1509fbfc2d62e0b40949e4245524a</hash>\n" +
            "</usertoken>";
}
