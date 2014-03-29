package net.whydah.identity.security;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import net.whydah.identity.user.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * User: asbkar
 */
public class SecurityTokenHelper {
    private static final Logger logger = LoggerFactory.getLogger(SecurityTokenHelper.class);
    private final WebResource wr;
    private String myAppTokenId;
    private String myAppTokenXML;
    private final boolean mock;

    public SecurityTokenHelper(String usertokenserviceUri) {
        if(!"mock".equals(usertokenserviceUri)) {
            Client client = Client.create();
            wr = client.resource(usertokenserviceUri);
            mock = false;
        } else {
            wr = null;
            mock = true;
        }
    }

    public UserToken getUserToken(String usertokenid) {
        if(mock) {
            return new UserToken("<token><fornavn>Stand</fornavn><etternavn>Alone</etternavn><application ID=\"1\"><organization ID=\"2\"><role name=\"WhydahUserAdmin\"/></organization></application></token>");
        }
        String appTokenId = getAppTokenId();
        logger.debug("usertokenid={}", usertokenid);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("usertokenid", usertokenid);
        formData.add("apptoken", myAppTokenXML);
        ClientResponse response = wr.path("token/" + appTokenId + "/getusertokenbytokenid").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        logger.info("Accessing:"+"token/" + appTokenId + "/getusertokenbytokenid");
        if(response.getStatus() == Response.Status.OK.getStatusCode()) {
            String usertoken = response.getEntity(String.class);
            logger.debug("usertoken: {}", usertoken);
            return new UserToken(usertoken);
        }
        logger.info("User token NOT ok: {}", response.getStatus()+response.toString());
        return null;

    }

    private String getAppTokenId() {
        if(myAppTokenId != null) {
            ClientResponse response = wr.path(myAppTokenId + "/validate").get(ClientResponse.class);
            if(response.getStatus() == Response.Status.OK.getStatusCode()) {
                logger.debug("Prev token ok");
                return myAppTokenId;
            }
        }
        logger.info("Must reauthenticate myself");
        authenticateme();
        return myAppTokenId;
    }

    private synchronized void authenticateme() {
        String auth = "<applicationcredential><params><applicationID>1</applicationID><applicationSecret>secret</applicationSecret></params></applicationcredential>";
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("applicationcredential", auth);
        String apptoken = wr.path("logon").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(String.class, formData);
        myAppTokenXML = apptoken;
        logger.info("apptoken={}", apptoken);
        myAppTokenId = getTokenIdFromAppToken(apptoken);
    }

    private String getTokenIdFromAppToken(String appTokenXML) {
        return appTokenXML.substring(appTokenXML.indexOf("<applicationtoken>") + "<applicationtoken>".length(), appTokenXML.indexOf("</applicationtoken>"));
    }

}
