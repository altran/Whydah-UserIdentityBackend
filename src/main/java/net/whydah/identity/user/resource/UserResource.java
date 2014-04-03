package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.application.role.Application;
import net.whydah.identity.application.role.ApplicationRepository;
import net.whydah.identity.user.UserService;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administration of users and their data.
 */
@Path("/{applicationtokenid}/{userTokenId}/user")
public class UserResource {
    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserIdentityService userIdentityService;
    private final UserService userService;
    private final ApplicationRepository applicationRepository;

    @Context
    private UriInfo uriInfo;

    @Inject
    public UserResource(UserIdentityService userIdentityService, UserService userService, ApplicationRepository applicationRepository) {
        this.userIdentityService = userIdentityService;
        this.userService = userService;
        this.applicationRepository = applicationRepository;
    }

    //Add user and add default roles
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(String userJson) {
        log.trace("addUser is called with userJson={}", userJson);
        try {
            WhydahUserIdentity whydahUserIdentity = userService.addUser(userJson);
            return Response.ok().build();   //TODO return whydahUserIdentity
        } catch (IllegalArgumentException iae) {
            log.error("addUser: Invalid json={}", userJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user details.
     *
     * @param username Username
     * @return user details and roles.
     */
    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        log.trace("getUser with username=" + username);

        WhydahUser user;
        try {
            user = userService.getUser(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        HashMap<String, Object> model = new HashMap<>(2);
        model.put("user", user);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/user.json.ftl", model)).build();
    }

    @PUT
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUser(@PathParam("username") String username, String userJson) {
        log.trace("updateUserIdentity: username={}, userJson={}", username, userJson);

        try {
            WhydahUserIdentity newUserIdentity = userService.updateUserIdentity(username, userJson);
            return Response.ok().build();   //TODO return whydahUserIdentity
        } catch (IllegalArgumentException iae) {
            log.error("modifyUser: Invalid json={}", userJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }


        /*
        try {
            WhydahUserIdentity whydahUserIdentity = userIdentityService.getUserinfo(username);
            if (whydahUserIdentity == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            try {
                JSONObject jsonobj = new JSONObject(userJson);
                //log.debug("jsonstr:"+userJson);
                //log.debug("json:"+jsonobj.toString());
                whydahUserIdentity.setFirstName(jsonobj.getString("firstName"));
                whydahUserIdentity.setLastName(jsonobj.getString("lastName"));
                whydahUserIdentity.setEmail(jsonobj.getString("email"));
                whydahUserIdentity.setCellPhone(jsonobj.getString("cellPhone"));
                whydahUserIdentity.setPersonRef(jsonobj.getString("personRef"));
                whydahUserIdentity.setUsername(jsonobj.getString("username"));
                log.debug("json:" + jsonobj.toString());
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            log.debug("Endret bruker: {}", whydahUserIdentity);
            userIdentityService.updateUser(username, whydahUserIdentity);
            indexer.update(whydahUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", whydahUserIdentity.toString());
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
        */
    }

    @DELETE
    @Path("/{username}")
    public Response deleteUser(@PathParam("username") String username) {
        try {
            userService.deleteUser(username);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (IllegalArgumentException iae) {
            log.error("deleteUser failed username={}", username + ". " + iae.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Path("/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for user {}", username);
        try {
            WhydahUserIdentity user = userIdentityService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userIdentityService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    //TODO Can modifyUser be used instead?
    @POST
    @Path("/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePasswordForUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Changing password for {}", username);
        try {
            WhydahUserIdentity user = userIdentityService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userIdentityService.authenticateWithTemporaryPassword(username, token);
            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                userIdentityService.changePassword(username, user.getUid(), newpassword);
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePasswordForUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
    @POST
    @Path("users/{username}/newuser/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Endrer data for ny bruker {}: {}", username, passwordJson);
        try {
            WhydahUserIdentity user = userIdentityService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userIdentityService.authenticateWithTemporaryPassword(username, token);
            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                String newusername = jsonobj.getString("newusername");
                if (!username.equals(newusername)) {
                    WhydahUserIdentity newidexists = userIdentityService.getUserinfo(newusername);
                    if (newidexists != null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build();
                    }
                    user.setUsername(newusername);
                    userIdentityService.updateUser(username, user);
                }
                userIdentityService.changePassword(newusername, user.getUid(), newpassword);
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            log.info("Nye brukerdata lagret");
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (NamingException e) {
            log.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    */


    /////////// User-application relation

    @POST
    @Path("/{username}/application")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addApplication(@PathParam("username") String username) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    @GET
    @Path("/{username}/application//{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplication(@PathParam("username") String username, @PathParam("applicationId") String applicationId) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    @PUT
    @Path("/{username}/application//{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyApplication(@PathParam("username") String username, @PathParam("applicationId") String applicationId) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    @DELETE
    @Path("/{username}/application//{applicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUserApplicationRelation(@PathParam("username") String username, @PathParam("applicationId") String applicationId) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    /**
     * Lister alle applikasjoner, samt angir om brukeren har noen roller her.
     *
     * @param username user id
     * @return app-liste.
     */
    @GET
    @Path("/{username}/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersApplications(@PathParam("username") String username) {
        WhydahUser user;
        try {
            user = userService.getUser(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        List<Application> allApps = applicationRepository.getApplications();
        Set<String> myApps = new HashSet<>();
        for (UserPropertyAndRole role : user.getPropsAndRoles()) {
            myApps.add(role.getAppId());
        }

        HashMap<String, Object> model = new HashMap<>(3);
        model.put("allApps", allApps);
        model.put("myApps", myApps);
        return Response.ok(new Viewable("/useradmin/userapps.json.ftl", model)).build();
    }

    @DELETE
    @Path("/{username}/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApplications(@PathParam("username") String username) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    /////////// Roles

    /*
    @GET
    @Path("users/{username}/{appid}/deleteall")
    public Response deleteAllUserRolesForApp(@PathParam("username") String username, @PathParam("appid") String appid) {
        log.debug("Fjern alle roller for {}: {}", username, appid);
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
            log.debug("fant8 {}", whydahUserIdentity);
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
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
    */


    /*
    @Path("users/{username}/{appid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(@PathParam("username") String username, @PathParam("appid") String appid) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
        } catch (NamingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        WhydahUser whydahUser = new WhydahUser(whydahUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid()));
        List<UserPropertyAndRole> rolesForApp = new ArrayList<>();
        for (UserPropertyAndRole role : whydahUser.getPropsAndRoles()) {
            if (role.getAppId().equals(appid)) {
                rolesForApp.add(role);
            }
        }
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("roller", rolesForApp);
        return Response.ok(new Viewable("/useradmin/roles.json.ftl", model)).build();
    }

    @POST
    @Path("users/{username}/{appid}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        log.debug("Fjern rolle for {} i app {}: {}", new String[]{username, appid, jsonrole});
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
            log.debug("fant bruker: {}", whydahUserIdentity);
        } catch (NamingException e) {
            log.error("", e);
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
            log.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }
    */

    /*
    @PUT
    @Path("users/{username}/{appid}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyRoleValue(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
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
            log.error("bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }
    */

     /*
    @POST
    @Path("users/{username}/{appid}/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        log.debug("legg til rolle for uid={}, appid={}, rollejson={}", new String[]{username, appid, jsonrole});
        if (jsonrole == null || jsonrole.trim().length() == 0) {
            log.warn("Empty json payload");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
            log.debug("fant6 {}", whydahUserIdentity);
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
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
            log.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        log.debug("Role: {}", role);
//        if(appid.equals(PstyrImporter.APPID_INVOICE) && !PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            log.warn("Ugyldig rolle for invoice");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
//        if(!appid.equals(PstyrImporter.APPID_INVOICE) && PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            log.warn("App og rolle matcher ikke");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
        String uid = whydahUserIdentity.getUid();
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            if (existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                log.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }
        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + uid + ", username=" + username + ", appid=" + appid + ", role=" + jsonrole);
        return Response.ok().build();
    }
    */

    /*
    @GET
    @Path("users/{username}/{appid}/adddefaultrole")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDefaultRole(@PathParam("username") String username, @PathParam("appid") String appid) {
        log.debug("legg til default rolle for {}:{}", username, appid);
        WhydahUserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserinfo(username);
            log.debug("fant7 {}", whydahUserIdentity);
        } catch (NamingException e) {
            log.error(e.getLocalizedMessage(), e);
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
        log.debug("Role: {}", role);
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        for (UserPropertyAndRole existingRole : existingRoles) {
            if (existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                log.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + whydahUserIdentity.getUid() + ", appid=" + appid + ", role=" + role);

        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("rolle", role);
        return Response.ok(new Viewable("/useradmin/role.json.ftl", model)).build();
    }
    */

}
