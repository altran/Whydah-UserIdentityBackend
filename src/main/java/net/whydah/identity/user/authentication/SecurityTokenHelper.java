package net.whydah.identity.user.authentication;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.identity.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class SecurityTokenHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenHelper.class);
    private final WebResource webResource;
    private String myAppTokenId;
    private String myAppTokenXML;

    public SecurityTokenHelper(String usertokenserviceUri) {
        Client client = Client.create();
        webResource = client.resource(usertokenserviceUri);
    }

    public UserToken getUserToken(String appTokenId, String usertokenid) {
        log.debug("usertokenid={}", usertokenid);
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("usertokenid", usertokenid);
        formData.add("apptoken", myAppTokenXML);
        try {
            ClientResponse response = webResource.path("token/" + appTokenId + "/getusertokenbyusertokenid").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
            log.info("Accessing:" + "tokenservice/" + appTokenId + "/getusertokenbyusertokenid");
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                String usertoken = response.getEntity(String.class);
                log.debug("usertoken: {}", usertoken);
                return new UserToken(usertoken);
            }
            log.warn("User token NOT ok: {}", response.getStatus() + response.toString());
            List<UserRole> roles = new ArrayList<>();
            roles.add(new UserRole("9999", "99999", "mockrole"));
            return new UserToken("MockUserToken", roles);
        } catch (ClientHandlerException che) {
            if (che.getCause() instanceof java.net.ConnectException) {
                log.error("Could not connect to SecurityTokenService to verify applicationTokenId {}, userTokenId {}", appTokenId, usertokenid);
                return null;
            } else {
                throw che;
            }

        }

    }

    private String getAppTokenId() {
        if (myAppTokenId != null) {
            ClientResponse response = webResource.path(myAppTokenId + "/validate").get(ClientResponse.class);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                log.debug("Prev token ok");
                return myAppTokenId;
            }
        }
        log.info("Must reauthenticate myself");
        authenticateme();
        return myAppTokenId;
    }

    private synchronized void authenticateme() {
        String auth = "<applicationcredential><params><applicationID>1</applicationID><applicationSecret>secret</applicationSecret></params></applicationcredential>";
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", auth);
        String apptoken = webResource.path("logon").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(String.class, formData);
        myAppTokenXML = apptoken;
        log.info("apptoken={}", apptoken);
        myAppTokenId = getTokenIdFromAppToken(apptoken);
    }

    private String getTokenIdFromAppToken(String appTokenXML) {
        return appTokenXML.substring(appTokenXML.indexOf("<applicationtoken>") + "<applicationtoken>".length(), appTokenXML.indexOf("</applicationtoken>"));
    }

}
