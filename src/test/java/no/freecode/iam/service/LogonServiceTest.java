package no.freecode.iam.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import no.freecode.iam.service.config.AppConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//import no.freecode.iam.service.prestyr.PstyrImporterTest;

public class LogonServiceTest {
    private static URI baseUri;
    Client restClient;
    private static Main uib;

    @BeforeClass
    public static void init() throws Exception {
        //PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_JUNIT);
        uib = new Main();
        uib.startEmbeddedDS();
        uib.importUsersAndRoles();
        uib.startHttpServer();
        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(uib.getPort()).build();
    }

    @AfterClass
    public static void cleanup() {
        uib.stop();
        //PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
    }

    @Before
    public void initRun() throws Exception {
        restClient = Client.create();
    }

    @Test
    public void welcome() {
        WebResource webResource = restClient.resource(baseUri);
        String s = webResource.get(String.class);
        assertTrue(s.contains("Freecode"));
        assertTrue(s.contains("<FORM"));
        assertFalse(s.contains("backtrace"));
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     */
    @Test
    public void testApplicationWadl() {
        WebResource webResource = restClient.resource(baseUri);
        String serviceWadl = webResource.path("application.wadl").
                accept(MediaTypes.WADL).get(String.class);
//        System.out.println("WADL:"+serviceWadl);
        assertTrue(serviceWadl.length() > 60);
    }

    @Test
    public void formLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("username", "sunil@freecode.no");
        formData.add("password", "654321");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
        //System.out.println(responseBody);
        //assertTrue(responseBody.contains("Logon ok"));
        assertTrue(responseBody.contains("sunil@freecode.no"));
    }

    @Test
    public void formLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("username", "sunil@freecode.no");
        formData.add("password", "vrangt");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
        //System.out.println(responseBody);

        assertTrue(responseBody.contains("failed"));
        assertFalse(responseBody.contains("freecodeUser"));
    }

    @Test
    public void XMLLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>sunil@freecode.no</username><coffee>yes please</coffee><password>654321</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("freecode"));

    }

    @Test
    public void XMLLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>sunil@freecode.no</username><coffee>yes please</coffee><password>vrangt</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("logonFailed"));
        assertFalse(responseXML.contains("freecodeUser"));
    }

}
