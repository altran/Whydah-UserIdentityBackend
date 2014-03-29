package net.whydah.identity.usertoken;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.email.PasswordSender;
import net.whydah.identity.user.identity.UserAuthenticationService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for authorization of users and finding WhydahUser with corresponding applications, organizations and roles.   
 */
@Path("/")
public class UserTokenResource {
    private static final Logger log = LoggerFactory.getLogger(UserTokenResource.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");


    private UserPropertyAndRoleRepository roleRepository;
    private UserAdminHelper userAdminHelper;
    private UserAuthenticationService userAuthenticationService;
    private Map<String, Object> welcomeModel;

    @Inject
    private PasswordSender passwordSender;

    @Inject
    private AuditLogRepository auditLogRepository;



    @Inject
    public UserTokenResource(UserPropertyAndRoleRepository roleRepository, UserAdminHelper userAdminHelper,
                             UserAuthenticationService userAuthenticationService) {
        this.roleRepository = roleRepository;
        this.userAdminHelper = userAdminHelper;
        this.userAuthenticationService = userAuthenticationService;
        createWelcomeModel();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        return Response.ok(new Viewable("/welcome", welcomeModel)).build();
    }


    /**
     * Authentication using XML. XML must contain an element with name username, and an element with name password.
     * @param input XML input stream.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @Path("logon")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateUser(InputStream input) {
        log.trace("authenticateUser XML");
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document dDoc = builder.parse(input);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String username = (String) xPath.evaluate("//username", dDoc, XPathConstants.STRING);
            String password = (String) xPath.evaluate("//password", dDoc, XPathConstants.STRING);
            String fbId = (String) xPath.evaluate("//fbId", dDoc, XPathConstants.STRING);

            String passwordCredentials;
            if (password != null && !password.equals("")) {
                passwordCredentials =  password;
            } else if (fbId != null && !fbId.equals("")) {
                passwordCredentials =  UserAdminHelper.calculateFacebookPassword(fbId);
            } else {
                log.info("Neither password nor facebookId is set. Returning " + Response.Status.FORBIDDEN);
                String entity = "<error>Error in input<error>";
                return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
            }

            return authenticateUser(username, passwordCredentials);
        } catch (Exception e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        }
    }

    Response authenticateUser(String username, String password) {
        WhydahUserIdentity id = userAuthenticationService.auth(username, password);

        if (id == null)  {
            Viewable entity = new Viewable("/logonFailed.xml.ftl");
            return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
        }

        List<UserPropertyAndRole> roles = roleRepository.getUserPropertyAndRoles(id.getUid());
        WhydahUser whydahUser = new WhydahUser(id, roles);
        log.info("Authentication ok for user {}", username);
        Viewable entity = new Viewable("/user.xml.ftl", whydahUser);
        return Response.ok(entity).build();
    }


    @Path("createandlogon")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndAuthenticateUser(InputStream input) {
        log.trace("createAndAuthenticateUser");

        Document fbUserDoc = parse(input);
        if (fbUserDoc == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }

        WhydahUserIdentity userIdentity = UserAdminHelper.createWhydahUserIdentity(fbUserDoc);

        if (userIdentity == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }


        String facebookUserAsString = getFacebookDataAsXmlString(fbUserDoc);
        //String facebookUserAsString = getFacebookDataAsXmlString(input);
        return createAndAuthenticateUser(userIdentity, facebookUserAsString,true);
    }

    public static String getFacebookDataAsXmlString(Document fbUserDoc) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.transform(new DOMSource(fbUserDoc), new StreamResult(buffer));
            String original = buffer.toString();

            // Wrap everything in CDATA
            return "<![CDATA[" + original + "]]>";
        } catch (Exception e) {
            log.error("Could not convert Document to string.", e);
            return null;
        }
    }

    public static String getFacebookDataAsString(InputStream input) {
        String facebookUserAsString = null;
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(input, Charsets.UTF_8);
            facebookUserAsString = CharStreams.toString(reader);
        } catch (IOException e) {
            log.warn("Error parsing inputStream as string.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.info("Could not close reader.");
                }
            }
        }
        log.debug("facebookUserAsString=" + facebookUserAsString);
        return facebookUserAsString;
    }

    Document parse(InputStream input) {
        Document fbUserDoc;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            fbUserDoc = builder.parse(input);
        } catch (Exception e) {
            log.error("Error when creating WhydahUserIdentity from incoming xml stream.", e);
            return null;
        }
        return fbUserDoc;
    }


    public Response createAndAuthenticateUser(WhydahUserIdentity userIdentity, String roleValue, boolean reuse) {
        try {
            Response response = userAdminHelper.addUser(userIdentity);
            if (!reuse && response.getStatus() != Response.Status.OK.getStatusCode()) {
                return response;
            }
            if (userIdentity!= null){

                userAdminHelper.addFacebookDataRole(userIdentity, roleValue);
            }

            return authenticateUser(userIdentity.getUsername(), userIdentity.getPassword());
        } catch (Exception e) {
            log.error("createAndAuthenticateUser failed " + userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        }
    }

    /**
     * Form/html-based authentication.
     * @param username Username to be authenticated.
     * @param password Users password.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @Path("logon")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response authenticateUserForm(@FormParam("username") String username, @FormParam("password") String password) {
        log.debug("authenticateUserForm: user=" + username + ", password=" + password);
        WhydahUserIdentity id = null;
        if (username != null && password != null) {
            id = userAuthenticationService.auth(username, password);
//            if(id == null) {
//                System.out.println("Prøver intern ldap");
//                id = internalLdapAuthenticator.auth(username, password);
//            }
        } else {
            log.warn("Missing user or password");
        }
        if (id == null) {
            return Response.ok(new Viewable("/logonFailed.ftl")).build();
        }
        WhydahUser whydahUser = new WhydahUser(id, roleRepository.getUserPropertyAndRoles(id.getUid()));


        return Response.ok(new Viewable("/user.ftl", whydahUser)).build();

    }

    private void createWelcomeModel() {
        welcomeModel = new HashMap<>(1);
        try {
            final String hostname = java.net.InetAddress.getLocalHost().getHostName();
            welcomeModel.put("hostname", hostname);
        } catch (UnknownHostException e) {
            log.warn("", e);
        }
    }


    @GET
    @Path("users/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for user {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);

            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            passwordSender.resetPassword(username, user.getEmail());
            audit(ActionPerformed.MODIFIED, "resetpassword", user.getUid());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void audit(String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }

    //Copy of changePasswordForUser in UserResource
    @POST
    @Path("users/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Changing password for {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userAuthenticationService.authenticateWithTemporaryPassword(username, token);

            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                userAuthenticationService.changePassword(username, newpassword);
                audit(ActionPerformed.MODIFIED, "password", user.getUid());
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
