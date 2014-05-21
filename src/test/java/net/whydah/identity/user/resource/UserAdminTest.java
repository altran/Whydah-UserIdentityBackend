package net.whydah.identity.user.resource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import net.whydah.identity.Main;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.user.email.MockMail;
import net.whydah.identity.util.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class UserAdminTest {
    private static WebResource baseResource;
    private static WebResource logonResource;
    private static Main uib;

    @Before
    public void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);
        FileUtils.deleteDirectory(new File("target/ssotest/"));
        FileUtils.deleteDirectory(new File("target/bootstrapdata/"));
        uib = new Main();
        uib.startEmbeddedDS();
        uib.importUsersAndRoles();
        uib.startHttpServer();
        URI baseUri = UriBuilder.fromUri("http://localhost/uib/uib/useradmin/").port(uib.getPort()).build();
        URI logonUri = UriBuilder.fromUri("http://localhost/uib/").port(uib.getPort()).build();
        //String authentication = "usrtk1";
        baseResource = Client.create().resource(baseUri)/*.path(authentication + '/')*/;
        logonResource = Client.create().resource(logonUri);
    }

    @After
    public void teardown() throws InterruptedException {
        uib.stop();

        FileUtils.deleteDirectory(new File("target/ssotest/"));
    }

    @Test
    public void find() {
        WebResource webResource = baseResource.path("users/find/Thomas");
        String s = webResource.get(String.class);
        assertTrue(s.contains("\"firstName\":\"Thomas\""));
    }

    @Test
    public void getuser() {
        WebResource webResource = baseResource.path("user/thomas.pringle@altran.com");
        String s = webResource.get(String.class);
        //System.out.println(s);
        assertTrue(s.contains("\"firstName\":\"Thomas\""));
    }

    @Test
    public void getnonexistinguser() {
        WebResource webResource = baseResource.path("user/");
        webResource.path("thomas.pringle@altran.com").get(String.class); // verify that path works with existing user
        try {
            String s = webResource.path("bantelonga@gmail.com").get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void modifyUser() {
        String uid = doAddUser("1231312", "siqula", "Hoytahl", "Goffse", "siqula@midget.orj", "12121212");

        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Hoytahl"));
        assertTrue(s.contains("12121212"));

        String updateduserjson = "{\n" +
                " \"uid\":\"" + uid + "\",\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"username\":\"siqula\",\n" +
                " \"firstName\":\"Harald\",\n" +
                " \"lastName\":\"Goffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"35353535\"\n" +
                "}";

        baseResource.path("user/" + uid).type("application/json").put(String.class, updateduserjson);

        s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Harald"));
        assertFalse(s.contains("Hoytahl"));
        assertTrue(s.contains("35353535"));
        assertFalse(s.contains("12121212"));
    }

    @Test
    public void deleteUser() {
        String uid = doAddUser("rubblebeard", "frustaalstrom", "Frustaal", "Strom", "frustaalstrom@gmail.com", "12121212");

        ClientResponse deleteResponse = baseResource.path("user/" + uid).delete(ClientResponse.class);
        deleteResponse.getClientResponseStatus().getFamily().equals(Response.Status.Family.SUCCESSFUL);

        try {
            String s = baseResource.path(uid).get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUserNotFound() {
        WebResource webResource = baseResource.path("users/dededede@hotmail.com/delete");
        try {
            String s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }

    }




    @Test
    public void getuserapps() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/applications");
        String s = webResource.get(String.class);
        //System.out.println(s);
        assertFalse(s.contains("\"firstName\":\"SUNIL\""));
        assertTrue(s.contains("\"appId\" : \"201\""));
        assertTrue(s.contains("\"appId\" : \"101\""));
        //assertTrue(s.contains("\"hasRoles\" : true"));
        assertTrue(s.contains("\"hasRoles\" : false"));
    }



    @Test
    public void getuserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        doAddUserRole(uid, "0005", "KK", "test");
        doAddUserRole(uid, "0005", "NN", "another");
        doAddUserRole(uid, "0005", "MM", "yetanother");

        Map<String, Object> testRole = doGetUserRole(uid, "test");
        assertEquals("0005", testRole.get("organizationId"));
        assertEquals("KK", testRole.get("applicationRoleName"));
        assertEquals("test", testRole.get("applicationRoleValue"));
    }


    @Test
    public void getuserroles() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        doAddUserRole(uid, "0005", "KK", "test");

        List<Map<String, Object>> rolesAfter = doGetUserRoles(uid);
        assertEquals("0005", rolesAfter.get(0).get("organizationId"));
        assertEquals("KK", rolesAfter.get(0).get("applicationRoleName"));
        assertEquals("test", rolesAfter.get(0).get("applicationRoleValue"));
    }


    @Test
    public void adduserrole() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");
        List<Map<String, Object>> rolesBefore = doGetUserRoles(uid);
        assertTrue(rolesBefore.isEmpty());

        doAddUserRole(uid, "0005", "KK", "test");

        List<Map<String, Object>> rolesAfter = doGetUserRoles(uid);
        assertEquals(1, rolesAfter.size());
    }

    @Test
    public void adduserroleNoJson() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "");
            //System.out.println(s);
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void adduserroleBadJson() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "{ dilldall }");
            //System.out.println(s);
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus());
        }
    }



    @Test
    public void addDefaultUserrole() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no");
        webResource.path("/adddefaultrole").get(ClientResponse.class);
        String s = webResource.get(String.class);
        //System.out.println(s);
        assertTrue(s.contains("useradmin"));
        WebResource webResource2 = baseResource.path("users/sunil@freecode.no");
        s = webResource2.get(String.class);
        //System.out.println("Roller etter: " + s);
        assertTrue(s.contains("useradmin"));
    }


    @Test
    public void addExistingUserrole() {
        WebResource webResource = baseResource.path("users/rafal.laczek@freecode.no/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "{\"orgID\": \"0001\",\n" +
                    "        \"roleName\": \"DEV\",\n" +
                    "        \"roleValue\": \"2012 - 2013\"}");
            //System.out.println(s);
            fail("Expected exception with 409, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    @Ignore
    public void deleteuserrole() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/50");
        String s = webResource.get(String.class);
        assertTrue(s.contains("test"));
        webResource.path("/delete").type("application/json").post(String.class, "{\"orgID\": \"0001\", \"roleName\": \"Dev\"}");
        s = webResource.get(String.class);
        assertFalse(s.contains("Dev"));
        WebResource webResource2 = baseResource.path("users/sunil@freecode.no");
        s = webResource2.get(String.class);
        assertFalse(s.contains("Dev"));
    }


    @Test
    @Ignore
    public void deleteAllAppuserroles() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/50");
        String s = webResource.get(String.class);
        assertTrue(s.contains("test"));
        webResource.path("/deleteall").get(String.class);
        WebResource webResource2 = baseResource.path("users/rafal.laczek@freecode.no");
        s = webResource2.get(String.class);
        assertFalse(s.contains("Dev"));
    }



    @Test
    public void modifyuserrole() {
        WebResource webResource = baseResource.path("users/rafal.laczek@freecode.no/201");
        String s = webResource.get(String.class);
        //System.out.println("S in modifyuserrole(): "+s);
        assertTrue(s.contains("DEV"));
        webResource.type("application/json").put(String.class, "{\"orgID\": \"0001\", \"roleName\": \"DEV\", \"roleValue\" : \"flott\"}");
        s = webResource.get(String.class);
        //System.out.println("S in modifyuserrole() is"+s);
        assertTrue(s.contains("flott"));
    }




    @Test
    public void userexists() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/exists");
        String s = webResource.get(String.class);
        assertTrue(s.contains("true"));
    }



    @Test
    public void usernotexists() {
        WebResource webResource = baseResource.path("users/eggbert@hotmail.com/exists");
        String s = webResource.get(String.class);
        assertTrue(s.contains("false"));
    }



    @Test
    public void addUser() {
        String uid = doAddUser("riffraff", "snyper", "Edmund", "Goffse", "snyper@midget.orj", "12121212");

        assertNotNull(uid);

        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
        s = baseResource.path("users/find/snyper").get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
    }

    @Test
    public void addStrangeUserWithMissingLastNameAndCodesInCellPhoneNumber() {

        // TODO Are users supposed to be added even with missing lastname?

        String uid = doAddUser("triffraff", "tsnyper", "tEdmund", null, "tsnyper@midget.orj", "12121-bb-212");

        // Get
        String ts = baseResource.path("users/find/tsnyper").get(String.class);
        assertTrue(ts.contains("tsnyper@midget.orj"));
        assertTrue(ts.contains("tEdmund"));


        // Search
        String s = baseResource.path("user/" + uid).get(String.class);
        assertTrue(s.contains("tsnyper@midget.orj"));
        assertTrue(s.contains("tEdmund"));
    }


    @Test
    public void resetAndChangePassword() {
        String userjson = "{\n" +
                " \"personRef\":\"123123123\",\n" +
                " \"username\":\"sneile\",\n" +
                " \"firstName\":\"Effert\",\n" +
                " \"lastName\":\"Huffse\",\n" +
                " \"email\":\"sneile@midget.orj\",\n" +
                " \"cellPhone\":\"21212121\"\n" +
                "}";
        WebResource webResource = baseResource.path("users/add");
        //webResource.type("application/json").post(String.class, userjson);
        webResource.type("application/json").post(ClientResponse.class, userjson);
        baseResource.path("users/sneile/resetpassword").type("application/json").get(ClientResponse.class);
        String token = uib.getInjector().getInstance(MockMail.class).getToken("sneile");
        assertNotNull(token);

        ClientResponse response = baseResource.path("users/sneile/newpassword/" + token).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,"{\"newpassword\":\"naLLert\"}");
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><auth><username>sneile</username><password>naLLert</password></auth>";
        response = logonResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String identity = response.getEntity(String.class);
        assertTrue(identity.contains("identity"));
        assertTrue(identity.contains("sneile"));
    }

    @Test
    @Ignore
    public void newUserFromEmail() {
        String userjson = "{\n" +
                " \"personRef\":\"43234321\",\n" +
                " \"username\":\"gvarvar@midget.orc\",\n" +
                " \"firstName\":\"Gvarveig\",\n" +
                " \"lastName\":\"Neskle\",\n" +
                " \"email\":\"gvarvar@midget.orc\",\n" +
                " \"cellPhone\":\"43434343\"\n" +
                "}";
        WebResource webResource = baseResource.path("users/add");
        webResource.type("application/json").post(ClientResponse.class, userjson);
        baseResource.path("users/gvarvar@midget.orc/resetpassword").type("application/json").get(ClientResponse.class);
        String token = uib.getInjector().getInstance(MockMail.class).getToken("gvarvar@midget.orc");
        assertNotNull(token);

        String newUserPayload = "{\"newpassword\":\"zePelin32\", \"newusername\":\"gvarnes\"}";
        ClientResponse response = baseResource.path("users/gvarvar@midget.orc/newuser/" + token).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, newUserPayload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><auth><username>gvarnes</username><password>zePelin32</password></auth>";
        response = logonResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String identity = response.getEntity(String.class);
        //System.out.println(identity);
        assertTrue(identity.contains("identity"));
        assertTrue(identity.contains("gvarnes"));
    }

    private String doAddUser(String userjson) {
        WebResource webResource = baseResource.path("user");
        ClientResponse postResponse = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, userjson);
        String postResponseJson = postResponse.getEntity(String.class);
        Map<String, Object> createdUser = null;
        try {
            createdUser = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (String) createdUser.get("uid");
    }

    private String doAddUser(String personRef, String username, String firstName, String lastName, String email, String cellPhone) {
        String userjson = "{\n" +
                " \"personRef\":\"" + personRef + "\",\n" +
                " \"username\":\"" + username + "\",\n" +
                " \"firstName\":\"" + firstName + "\",\n" +
                " \"lastName\":\"" + lastName + "\",\n" +
                " \"email\":\"" + email + "\",\n" +
                " \"cellPhone\":\"" + cellPhone + "\"\n" +
                "}";
        return doAddUser(userjson);
    }

    private List<Map<String, Object>> doGetUserRoles(String uid) {
        String postResponseJson = baseResource.path("user/" + uid + "/roles").get(String.class);
        List<Map<String, Object>> roles = null;
        try {
            roles = new ObjectMapper().readValue(postResponseJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return roles;
    }

    private Map<String, Object> doGetUserRole(String uid, String applicationRoleName) {
        String postResponseJson = baseResource.path("user/" + uid + "/" + applicationRoleName).get(String.class);
        Map<String, Object> roles = null;
        try {
            roles = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return roles;
    }

    private Map<String, Object> doAddUserRole(String uid, String organizationId, String applicationRoleName, String applicationRoleValue) {
        WebResource webResource = baseResource.path("user/" + uid + "/role");
        ClientResponse postResponse = webResource.type("application/json").post(ClientResponse.class, "{\"organizationId\": \"" + organizationId + "\",\n" +
                "        \"applicationRoleName\": \"" + applicationRoleName + "\",\n" +
                "        \"applicationRoleValue\": \"" + applicationRoleValue + "\"}");

        String postResponseJson = postResponse.getEntity(String.class);
        Map<String, Object> createdUser = null;
        try {
            createdUser = new ObjectMapper().readValue(postResponseJson, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return createdUser;
    }

}