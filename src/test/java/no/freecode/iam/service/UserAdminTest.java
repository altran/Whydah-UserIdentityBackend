package no.freecode.iam.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import no.freecode.iam.service.config.AppConfig;
import no.freecode.iam.service.helper.FileUtils;
import no.freecode.iam.service.mail.MockMail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;


@Ignore
public class UserAdminTest {
    private static WebResource baseResource;
    private static WebResource logonResource;
    private static Main uib;
    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_JUNIT);
        FileUtils.deleteDirectory(new File("/tmp/ssotest/"));
        uib = new Main();
        uib.startEmbeddedDS();
        uib.importUsersAndRoles();
        uib.startHttpServer();
        URI baseUri = UriBuilder.fromUri("http://localhost/uib/useradmin/").port(uib.getPort()).build();
        URI logonUri = UriBuilder.fromUri("http://localhost/uib/").port(uib.getPort()).build();
        //String usertoken = "usrtk1";
        baseResource = Client.create().resource(baseUri)/*.path(usertoken + '/')*/;
        logonResource = Client.create().resource(logonUri);
    }

    @AfterClass
    public static void teardown() throws Exception {
        uib.stop();
        FileUtils.deleteDirectory(new File("/tmp/ssotest/"));
    }

    @Test
    public void getuser() {
        WebResource webResource = baseResource.path("users/rafal.laczek@freecode.no");
        String s = webResource.get(String.class);
        //System.out.println(s);
        assertTrue(s.contains("\"firstName\":\"RAFAL\""));
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
    public void getuserroles() {
        WebResource webResource = baseResource.path("users/rafal.laczek@freecode.no/201");
        String s = webResource.get(String.class);
        //System.out.println("S in getuserroles() is "+s+ "Stop");
        assertFalse(s.contains("\"firstName\":\"RAFAL\""));
        assertTrue(s.contains("\"appId\": \"201\""));
    }


    @Test
    public void adduserrole() {
        WebResource webResource = baseResource.path("users/sunil@freecode.no/50");
        webResource.path("/add").type("application/json").post(ClientResponse.class, "{\"orgID\": \"0005\",\n" +
                "        \"roleName\": \"KK\",\n" +
                "        \"roleValue\": \"test\"}");
        String s = webResource.get(String.class);
        assertTrue(s.contains("KK"));
        WebResource webResource2 = baseResource.path("users/sunil@freecode.no");
        s = webResource2.get(String.class);
//        System.out.println("Roller etter: " + s);
        assertTrue(s.contains("KK"));
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
    public void getnonexistinguser() {
        WebResource webResource = baseResource.path("users/bantelonga@gmail.com");
        try {
            String s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }


    @Test
    public void addUser() {

        String userjson = "{\n" +
                " \"personRef\":\"riffraff\",\n" +
                " \"username\":\"snyper\",\n" +
                " \"firstName\":\"Edmund\",\n" +
                " \"lastName\":\"Goffse\",\n" +
                " \"email\":\"snyper@midget.orj\",\n" +
                " \"cellPhone\":\"12121212\"\n" +
                "}";


        //WebResource webResource = baseResource.path("users/add"); //OLD

        WebResource webResource = baseResource.path("users/add");
        webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, userjson);

        // webResource.type(MediaType.APPLICATION_JSON).post(String.class,testStr);
        //webResource.type(MediaType.APPLICATION_JSON).post(String.class, userjson); //Old
        String s = baseResource.path("users/snyper").get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
        s = baseResource.path("find/snyper").get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
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

    @Test
    @Ignore
    public void modifyUser() {
        String userjson = "{\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"username\":\"siqula\",\n" +
                " \"firstName\":\"Hoytahl\",\n" +
                " \"lastName\":\"Goffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"12121212\"\n" +
                "}";
        String updateduserjson = "{\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"username\":\"siqula\",\n" +
                " \"firstName\":\"Harald\",\n" +
                " \"lastName\":\"Goffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"35353535\"\n" +
                "}";
        // baseResource.path("users/add").type("application/json").post(ClientResponse.class, userjson);
        baseResource.path("users/add").type("application/json").post(ClientResponse.class, userjson);
        String s = baseResource.path("users/siqula").get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Hoytahl"));
        assertTrue(s.contains("12121212"));

        baseResource.path("users/siqula").type("application/json").put(String.class, updateduserjson);
        s = baseResource.path("users/siqula").get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Harald"));
        assertTrue(s.contains("35353535"));
    }

    @Test
    public void find() {
        WebResource webResource = baseResource.path("find/Rafal");
        String s = webResource.get(String.class);
        assertTrue(s.contains("\"firstName\":\"RAFAL\""));
    }

    @Test
    public void deleteUser() {
        WebResource webResource = baseResource.path("users/frustaalstrom@gmail.com");
        //String s = webResource.get(String.class);
        String s = webResource.toString();

        assertTrue(s.contains("frustaalstrom"));
        webResource.path("/delete").get(ClientResponse.class);
        try {
            s = webResource.get(String.class);
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


}