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

public class SecurityTokenServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceHelper.class);
    private final WebResource tokenServiceResource;
    private String myAppTokenXML;
    //private String myAppTokenId;

    public SecurityTokenServiceHelper(String usertokenserviceUri) {
        tokenServiceResource = Client.create().resource(usertokenserviceUri);
    }

    public UserToken getUserToken(String appTokenId, String usertokenid) {
        log.debug("usertokenid={}", usertokenid);
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("usertokenid", usertokenid);
        formData.add("apptoken", myAppTokenXML);    //TODO myAppTokenXML is never set...
        try {
            ClientResponse response = tokenServiceResource.path("user").path(appTokenId).path("get_usertoken_by_usertokenid").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
            log.info("Accessing:" + "tokenservice/user/" + appTokenId + "/get_usertoken_by_usertokenid");
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                String usertoken = response.getEntity(String.class);
                log.debug("usertoken: {}", usertoken);
                return new UserToken(usertoken);
            }
            log.warn("User token NOT ok: {} {}", response.getStatus(), response.toString());
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
    /*
    private String getAppTokenId() {
        if (myAppTokenId != null) {
            ClientResponse response = tokenServiceResource.path(myAppTokenId + "/validate").get(ClientResponse.class);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                log.trace("Previous applicationtoken is ok");
                return myAppTokenId;
            }
        }
        log.warn("Previous applicationtoken  invalid - Must re-authenticate myself");
        authenticateMyself();
        return myAppTokenId;
    }


    private synchronized void authenticateMyself() {
        String auth = "<applicationcredential><params><applicationID>1</applicationID><applicationSecret>secret</applicationSecret></params></applicationcredential>";
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", auth);
        String apptoken = tokenServiceResource.path("logon").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(String.class, formData);
        myAppTokenXML = apptoken;
        log.info("apptoken={}", apptoken);
        myAppTokenId = getApplicationTokenIdFromAppToken(apptoken);
    }


    // TODO  change parsing to xpath parsing
    private String getApplicationTokenIdFromAppToken(String appTokenXML) {
        return appTokenXML.substring(appTokenXML.indexOf("<applicationtoken>") + "<applicationtoken>".length(), appTokenXML.indexOf("</applicationtoken>"));
    }
    */
}
