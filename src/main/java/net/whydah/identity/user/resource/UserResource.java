package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.application.role.Application;
import net.whydah.identity.application.role.ApplicationRepository;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.security.Authentication;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.identity.UserAuthenticationService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import net.whydah.identity.user.search.Indexer;
import net.whydah.identity.user.search.Search;
import net.whydah.identity.usertoken.UserToken;
import net.whydah.identity.util.PasswordGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Grensesnitt for brukeradministrasjon.
 */
@Path("/useradmin/{usertokenid}/")
public class UserResource {
    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Inject
    private UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    @Inject
    private ApplicationRepository applicationRepository;
    @Inject
    private Search search;
    @Inject
    private Indexer indexer;
    @Inject
    private AuditLogRepository auditLogRepository;
    @Inject
    private PasswordGenerator passwordGenerator;
    @Inject
    private UserAdminHelper userAdminHelper;

    private UserAuthenticationService userAuthenticationService;

    @Context
    private UriInfo uriInfo;

    @Inject
    public UserResource(UserAuthenticationService userAuthenticationService) {
        this.userAuthenticationService = userAuthenticationService;
    }

    //////////////// Users

    /**
     * Find users.
     *
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("find/{q}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("q") String query) {
        logger.debug("find with query=" + query);
        List<WhydahUserIdentity> result = search.search(query);

        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("users", result);
        logger.info("users", result);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/users.json.ftl", model)).build();
    }

    @GET
    @Path("users/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        logger.info("Reset password for user {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userAuthenticationService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Get user details.
     *
     * @param username Username
     * @return user details and roles.
     */
    @Path("users/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        logger.debug("getUser with username=" + username);

        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        WhydahUser whydahUser = new WhydahUser(whydahUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid()));
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("user", whydahUser);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/user.json.ftl", model)).build();
    }

    //Add user and add default roles
    @POST
    @Path("users/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(String userJson) {
        logger.debug("addUser: {}", userJson);
        try {
            JSONObject jsonobj = new JSONObject(userJson);
            WhydahUserIdentity userIdentity = new WhydahUserIdentity();
            String username = jsonobj.getString("username");
            logger.debug("Username is : " + username);
            InternetAddress internetAddress = new InternetAddress();
            String email = jsonobj.getString("email");
            if(email.contains("+")){
                email = replacePlusWithEmpty(email);
            }
            internetAddress.setAddress(email);
            try {
                internetAddress.validate();
                userIdentity.setEmail(jsonobj.getString("email"));
            } catch (AddressException e) {

                logger.error(String.format("E-mail: %s is of wrong format.", email));
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            userIdentity.setUsername(username);
            userIdentity.setFirstName(jsonobj.getString("firstName"));
            userIdentity.setLastName(jsonobj.getString("lastName"));

            userIdentity.setCellPhone(jsonobj.getString("cellPhone"));
            userIdentity.setPersonRef(jsonobj.getString("personRef"));
            //userIdentity.setUid(UUID.randomUUID().toString());
            userIdentity.setPassword(passwordGenerator.generate());

            return userAdminHelper.addUser(userIdentity);
        } catch (JSONException e) {
            logger.error("Bad json: " + userJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
    private String replacePlusWithEmpty(String email){
        String[] words = email.split("[+]");
        if(words.length == 1){
            return email;
        }
        email  = "";
        for (String word : words) {
            email += word;
        }
        return email;
    }

    private void audit(String action, String what, String value) {
        UserToken authenticatedUser = Authentication.getAuthenticatedUser();
        if (authenticatedUser == null) {
            logger.error("Audit log was not updated because authenticatedUser is not set. Check SecurityFilter.SECURED_PATHS_PARAM.");
            return;
        }

        String user = authenticatedUser.getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(user, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }

    @Path("users/{username}/exists")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response exists(@PathParam("username") String username) {
        logger.debug("does {} exist?");
        try {
            WhydahUserIdentity id = userAuthenticationService.getUserinfo(username);
            String result = (id != null) ? "{\"exists\" : true}" : "{\"exists\" : false}";
            logger.debug("exists: " + result);
            return Response.ok(result).build();
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("users/{username}/delete")
    public Response deleteUser(@PathParam("username") String username) {
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            userAuthenticationService.deleteUser(username);
            String uid = user.getUid();
            userPropertyAndRoleRepository.deleteUser(uid);
            indexer.removeFromIndex(uid);
            audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + username);
            return Response.ok().build();
        } catch (NamingException e) {
            logger.error("deleteUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("users/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUser(@PathParam("username") String username, String userJson) {
        logger.debug("modifyUser: ", userJson);
        try {
            WhydahUserIdentity whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            if (whydahUserIdentity == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            try {
                JSONObject jsonobj = new JSONObject(userJson);
                //logger.debug("jsonstr:"+userJson);
                //logger.debug("json:"+jsonobj.toString());
                whydahUserIdentity.setFirstName(jsonobj.getString("firstName"));
                whydahUserIdentity.setLastName(jsonobj.getString("lastName"));
                whydahUserIdentity.setEmail(jsonobj.getString("email"));
                whydahUserIdentity.setCellPhone(jsonobj.getString("cellPhone"));
                whydahUserIdentity.setPersonRef(jsonobj.getString("personRef"));
                whydahUserIdentity.setUsername(jsonobj.getString("username"));
                logger.debug("json:" + jsonobj.toString());
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            logger.debug("Endret bruker: {}", whydahUserIdentity);
            userAuthenticationService.updateUser(username, whydahUserIdentity);
            indexer.update(whydahUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", whydahUserIdentity.toString());
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }


    @POST
    @Path("users/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePasswordForUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        logger.info("Changing password for {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userAuthenticationService.authenticateWithTemporaryPassword(username, token);
            if (!ok) {
                logger.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                userAuthenticationService.changePassword(username, user.getUid(), newpassword);
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("changePasswordForUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("users/{username}/newuser/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        logger.info("Endrer data for ny bruker {}: {}", username, passwordJson);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userAuthenticationService.authenticateWithTemporaryPassword(username, token);
            if (!ok) {
                logger.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                String newusername = jsonobj.getString("newusername");
                if (!username.equals(newusername)) {
                    WhydahUserIdentity newidexists = userAuthenticationService.getUserinfo(newusername);
                    if (newidexists != null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build();
                    }
                    user.setUsername(newusername);
                    userAuthenticationService.updateUser(username, user);
                }
                userAuthenticationService.changePassword(newusername, user.getUid(), newpassword);
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            logger.info("Nye brukerdata lagret");
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (NamingException e) {
            logger.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /////////// Roles

    /**
     * Lister alle applikasjoner, samt angir om brukeren har noen roller her.
     *
     * @param username user id
     * @return app-liste.
     */
    @Path("users/{username}/applications")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(@PathParam("username") String username) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        WhydahUser whydahUser = new WhydahUser(whydahUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid()));
        List<Application> allApps = applicationRepository.getApplications();
        Set<String> myApps = new HashSet<String>();
        for (UserPropertyAndRole role : whydahUser.getPropsAndRoles()) {
            myApps.add(role.getAppId());
        }

        HashMap<String, Object> model = new HashMap<String, Object>(3);
        model.put("allApps", allApps);
        model.put("myApps", myApps);
        return Response.ok(new Viewable("/useradmin/userapps.json.ftl", model)).build();
    }

    @Path("users/{username}/{appid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(@PathParam("username") String username, @PathParam("appid") String appid) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        WhydahUser whydahUser = new WhydahUser(whydahUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid()));
        List<UserPropertyAndRole> rolesForApp = new ArrayList<UserPropertyAndRole>();
        for (UserPropertyAndRole role : whydahUser.getPropsAndRoles()) {
            if (role.getAppId().equals(appid)) {
                rolesForApp.add(role);
            }
        }
        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("roller", rolesForApp);
        return Response.ok(new Viewable("/useradmin/roles.json.ftl", model)).build();
    }

    @POST
    @Path("users/{username}/{appid}/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        logger.debug("legg til rolle for uid={}, appid={}, rollejson={}", new String[]{username, appid, jsonrole});
        if (jsonrole == null || jsonrole.trim().length() == 0) {
            logger.warn("Empty json payload");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            logger.debug("fant6 {}", whydahUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(whydahUserIdentity.getUid());
        role.setAppId(appid);
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            role.setOrgId(jsonobj.getString("orgID"));
            role.setRoleName(jsonobj.getString("roleName"));
            role.setRoleValue(jsonobj.getString("roleValue"));
        } catch (JSONException e) {
            logger.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.debug("Role: {}", role);
//        if(appid.equals(PstyrImporter.APPID_INVOICE) && !PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            logger.warn("Ugyldig rolle for invoice");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
//        if(!appid.equals(PstyrImporter.APPID_INVOICE) && PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            logger.warn("App og rolle matcher ikke");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
        String uid = whydahUserIdentity.getUid();
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            if (existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                logger.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }
        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + uid + ", username=" + username + ", appid=" + appid + ", role=" + jsonrole);
        return Response.ok().build();
    }

    @GET
    @Path("users/{username}/{appid}/adddefaultrole")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDefaultRole(@PathParam("username") String username, @PathParam("appid") String appid) {
        logger.debug("legg til default rolle for {}:{}", username, appid);
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            logger.debug("fant7 {}", whydahUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }

        Application app = applicationRepository.getApplication(appid);
        if (app == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"app not found\"}'").build();
        }
        if (app.getDefaultrole() == null) {
            return Response.status(Response.Status.CONFLICT).entity("{\"error\":\"app has no default role\"}'").build();
        }
        String orgName = userPropertyAndRoleRepository.getOrgname(app.getDefaultOrgid());
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(whydahUserIdentity.getUid());
        role.setAppId(appid);
        role.setApplicationName(app.getName());
        role.setOrgId(app.getDefaultOrgid());
        role.setOrganizationName(orgName);
        role.setRoleName(app.getDefaultrole());
        logger.debug("Role: {}", role);
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        for (UserPropertyAndRole existingRole : existingRoles) {
            if (existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                logger.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + whydahUserIdentity.getUid() + ", appid=" + appid + ", role=" + role);

        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("rolle", role);
        return Response.ok(new Viewable("/useradmin/role.json.ftl", model)).build();
    }

    @POST
    @Path("users/{username}/{appid}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        logger.debug("Fjern rolle for {} i app {}: {}", new String[]{username, appid, jsonrole});
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            logger.debug("fant bruker: {}", whydahUserIdentity);
        } catch (NamingException e) {
            logger.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            String orgid = jsonobj.getString("orgID");
            String rolename = jsonobj.getString("roleName");
            String uid = whydahUserIdentity.getUid();
            userPropertyAndRoleRepository.deleteUserRole(uid, appid, orgid, rolename);
            audit(ActionPerformed.DELETED, "role", "uid=" + uid + ", appid=" + appid + ", role=" + jsonrole);
        } catch (JSONException e) {
            logger.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("users/{username}/{appid}/deleteall")
    public Response deleteAllUserRolesForApp(@PathParam("username") String username, @PathParam("appid") String appid) {
        logger.debug("Fjern alle roller for {}: {}", username, appid);
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
            logger.debug("fant8 {}", whydahUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        String uid = whydahUserIdentity.getUid();
        userPropertyAndRoleRepository.deleteUserAppRoles(uid, appid);
        audit(ActionPerformed.DELETED, "role", "uid=" + uid + ", appid=" + appid + ", roles=all");
        return Response.ok().build();
    }


    @PUT
    @Path("users/{username}/{appid}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyRoleValue(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userAuthenticationService.getUserinfo(username);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            String orgid = jsonobj.getString("orgID");
            String rolename = jsonobj.getString("roleName");
            String rolevalue = jsonobj.getString("roleValue");
            String uid = whydahUserIdentity.getUid();
            userPropertyAndRoleRepository.updateUserRoleValue(uid, appid, orgid, rolename, rolevalue);
            audit(ActionPerformed.MODIFIED, "role", "uid=" + uid + ", appid=" + appid + ", role=" + jsonrole);
        } catch (JSONException e) {
            logger.error("bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

}
