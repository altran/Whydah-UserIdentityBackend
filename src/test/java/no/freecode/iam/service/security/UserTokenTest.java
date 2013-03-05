package no.freecode.iam.service.security;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UserTokenTest {
    @Test
    public void hasRole() {
        UserToken token = new UserToken(usertoken);
        assertTrue(token.hasRole("Brukeradmin"));
        assertFalse(token.hasRole("nesevis"));
    }
    @Test
    public void getRoles() {
        UserToken token = new UserToken(usertoken);

        UserRole expectedRole1 = new UserRole("1", "9999", "Brukeradmin");
        UserRole expectedRole2 = new UserRole("1", "9999", "Tester");
        UserRole expectedRole3 = new UserRole("005", "1234", "Brukeradmin");
        List<UserRole> actualRoles = token.getUserRoles();
        assertNotNull(actualRoles);
        assertEquals(3, actualRoles.size());
        assertTrue(actualRoles.contains(expectedRole1));
        assertTrue(actualRoles.contains(expectedRole2));
        assertTrue(actualRoles.contains(expectedRole3));
    }

    private final static String usertoken = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<token xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"b035df2e-e766-4077-a514-2c370cc78714\">\n" +
            "    <securitylevel>1</securitylevel>\n" +
            "    <personid></personid>\n" +
            "    <fornavn>Bruker</fornavn>\n" +
            "    <etternavn>Admin</etternavn>\n" +
            "    <timestamp>1299848579653</timestamp>\n" +
            "    <lifespan>200000</lifespan>\n" +
            "    <issuer>http://10.10.3.88:9998/iam/9056ac3f744957ae6a86daffb5aa98d3/usertoken</issuer>\n" +
            "    <application ID=\"1\">\n" +
            "        <applicationName>Brukeradmin</applicationName>\n" +
            "        <organization ID=\"9999\">\n" +
            "            <organizationName>OBOS</organizationName>\n" +
            "            <role name=\"Brukeradmin\" value=\"\"/>\n" +
            "            <role name=\"Tester\" value=\"\"/>\n" +
            "        </organization>\n" +
            "    </application>\n" +
            "    <application ID=\"005\">\n" +
            "        <applicationName>HMS</applicationName>\n" +
            "        <organization ID=\"1234\">\n" +
            "            <organizationName>NBBL</organizationName>\n" +
            "            <role name=\"Brukeradmin\" value=\"\"/>\n" +
            "        </organization>\n" +
            "    </application>\n" +
            "\n" +
            "    <ns2:link type=\"application/xml\" href=\"/b035df2e-e766-4077-a514-2c370cc78714\" rel=\"self\"/>\n" +
            "    <hash type=\"MD5\">6660ae2fcaa0b8311661fa9e3234eb7a</hash>\n" +
            "</token>";
}
