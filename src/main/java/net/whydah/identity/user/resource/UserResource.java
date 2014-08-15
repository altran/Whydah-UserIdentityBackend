package net.whydah.identity.user.resource;

import com.google.inject.Inject;
import com.sun.jersey.api.ConflictException;
import net.whydah.identity.application.ApplicationRepository;
import net.whydah.identity.user.InvalidRoleModificationException;
import net.whydah.identity.user.NonExistentRoleException;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.InvalidUserIdentityFieldException;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.role.UserPropertyAndRole;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

/**
 * Administration of users and their data.
 */
@Path("/{applicationtokenid}/{userTokenId}/user")
public class UserResource {
    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserIdentityService userIdentityService;
    private final UserAggregateService userAggregateService;
    private final ApplicationRepository applicationRepository;

    private final ObjectMapper mapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    public UserResource(UserIdentityService userIdentityService, UserAggregateService userAggregateService, ApplicationRepository applicationRepository) {
        this.userIdentityService = userIdentityService;
        this.userAggregateService = userAggregateService;
        this.applicationRepository = applicationRepository;
        this.mapper = new ObjectMapper();
    }

    /**
     * Expectations to input:
     * no UID
     * no password
     *
     * Output:
     * uid is included
     * no password
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserIdentity(String userIdentityJson) {
        log.trace("addUserIdentity, userIdentityJson={}", userIdentityJson);

        UserIdentityRepresentation representation;
        try {
            representation = mapper.readValue(userIdentityJson, UserIdentityRepresentation.class);
        } catch (IOException e) {
            log.error("addUserIdentity, invalid json. userIdentityJson={}", userIdentityJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UserIdentity userIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(representation);

            String newUserAsJson;
            try {
                newUserAsJson = mapper.writeValueAsString(userIdentity);
            } catch (IOException e) {
                log.error("Error converting to json. {}", userIdentity.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            //TODO Ensure password is not returned. Expect UserAdminService to trigger resetPassword.
            return Response.status(Response.Status.CREATED).entity(newUserAsJson).build();
        }  catch (ConflictException ise) {
            log.info("addUserIdentity returned {}, json={}", Response.Status.CONFLICT.toString(), userIdentityJson, ise);
            return Response.status(Response.Status.CONFLICT).build();
        } catch (IllegalArgumentException iae) {
            log.info("addUserIdentity returned {}, json={}", Response.Status.BAD_REQUEST.toString(), userIdentityJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (InvalidUserIdentityFieldException e) {
            log.info("addUserIdentity returned {} because {}", Response.Status.BAD_REQUEST.toString(), e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            log.error("addUserIdentity-RuntimeExeption ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserIdentity(@PathParam("uid") String uid) {
        log.trace("getUserIdentity, uid={}", uid);

        UserIdentity userIdentity;
        try {
            userIdentity = userIdentityService.getUserIndentityForUid(uid);
        } catch (NamingException e) {
            throw new RuntimeException("getUserIndentityForUid, uid=" + uid, e);
        }
        if (userIdentity == null) {
            log.trace("getUserIndentityForUid could not find user with uid={}", uid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String json;
        try {
            json = mapper.writeValueAsString(userIdentity);
        } catch (IOException e) {
            log.error("Error converting to json. {}", userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @PUT
    @Path("/{uid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserIdentity(@PathParam("uid") String uid, String userIdentityJson) {
        log.trace("updateUserIdentity: uid={}, userIdentityJson={}", uid, userIdentityJson);

        UserIdentity userIdentity;
        try {
            userIdentity = mapper.readValue(userIdentityJson, UserIdentity.class);
        } catch (IOException e) {
            log.error("updateUserIdentityForUsername, invalid json. userIdentityJson={}", userIdentityJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UserIdentity updatedUserIdentity = userAggregateService.updateUserIdentity(uid, userIdentity);

            try {
                String json = mapper.writeValueAsString(updatedUserIdentity);
                return Response.ok(json).build();
            } catch (IOException e) {
                log.error("Error converting to json. {}", userIdentity.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (InvalidUserIdentityFieldException iuife) {
            log.warn("updateUserIdentity returned {} because {}.", Response.Status.BAD_REQUEST.toString(), iuife.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalArgumentException iae) {
            log.info("updateUserIdentity: Invalid json={}", userIdentityJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("updateUserIdentity: RuntimeError json={}", userIdentityJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{uid}")
    public Response deleteUserAggregate(@PathParam("uid") String uid) {
        log.trace("deleteUserAggregate: uid={}", uid);

        try {
            userAggregateService.deleteUserAggregateByUid(uid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (IllegalArgumentException iae) {
            log.error("deleteUserIdentity failed username={}", uid + ". " + iae.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }



    // ROLES


    @POST
    @Path("/{uid}/role/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRole(@PathParam("uid") String uid, String roleJson) {
        log.trace("addRole, roleJson={}", roleJson);

        RoleRepresentationRequest request;
        try {
            request = mapper.readValue(roleJson, RoleRepresentationRequest.class);
        } catch (IOException e) {
            log.error("addRole, invalid json. roleJson={}", roleJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UserPropertyAndRole updatedRole = userAggregateService.addRole(uid, request);

            String json;
            try {
                json = mapper.writeValueAsString(updatedRole);
            } catch (IOException e) {
                log.error("Error converting to json. {}", updatedRole.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Response.Status.CREATED).entity(json).build();
        }  catch (ConflictException ce) {
            log.error("addRole-Conflict. {}", roleJson, ce);
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("addRole-RuntimeException. {}", roleJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{uid}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoles(@PathParam("uid") String uid) {
        log.trace("getRoles, uid={}", uid);

        List<UserPropertyAndRole> roles = userAggregateService.getRoles(uid);

        String json;
        try {
            json = mapper.writeValueAsString(roles);
        } catch (IOException e) {
            log.error("Error converting List<UserPropertyAndRole> to json. ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @GET
    @Path("/{uid}/role/{roleid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid) {
        log.trace("getRole, uid={}, roleid={}", uid, roleid);

        UserPropertyAndRole role = userAggregateService.getRole(uid, roleid);
        if (role == null) {
            log.trace("getRole could not find role with roleid={}", roleid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String json;
        try {
            json = mapper.writeValueAsString(role);
        } catch (IOException e) {
            log.error("Error converting to json. {}", role.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }

    @PUT
    @Path("/{uid}/role/{roleid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid, String roleJson) {
        log.trace("updateRole, uid={}, roleid={}", uid, roleid);

        UserPropertyAndRole role;
        try {
            role = mapper.readValue(roleJson, UserPropertyAndRole.class);
        } catch (IOException e) {
            log.error("updateRole, invalid json. roleJson={}", roleJson, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String json;
            UserPropertyAndRole updatedRole = userAggregateService.updateRole(uid, roleid, role);
            try {
                json = mapper.writeValueAsString(updatedRole);
            } catch (IOException e) {
                log.error("Error converting to json. {}", updatedRole.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok(json).build();
        } catch (NonExistentRoleException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (InvalidRoleModificationException e) {
            return Response.status(Response.Status.fromStatusCode(422)).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            log.error("updateRole-RuntimeException. {}", roleJson, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{uid}/role/{roleid}")
    public Response deleteRole(@PathParam("uid") String uid, @PathParam("roleid") String roleid) {
        log.trace("deleteRole, uid={}, roleid={}", uid, roleid);

        try {
            userAggregateService.deleteRole(uid, roleid);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (RuntimeException e) {
            log.error("deleteRole-RuntimeException. roleId {}", roleid, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Path("/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for user {}", username);
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);
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

    //TODO Can updateUserIdentityForUsername be used instead?
    @POST
    @Path("/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePasswordForUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Changing password for {}", username);
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok;
            try {
                ok = userIdentityService.authenticateWithChangePasswordToken(username, token);
            } catch (RuntimeException re) {
                log.error("changePasswordForUser-RuntimeException username {}, message {}", username,re.getMessage(), re);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

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

    @POST
    @Path("/{username}/changepassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePasswordbyAdmin(@PathParam("applicationtokenid") String applicationtokenid, @PathParam("userTokenId") String adminUserTokenId,
            @PathParam("username") String username, String password) {
        log.info("Admin Changing password for {}", username);
        //FIXME baardl: implement verification that admin is allowed to update this password.
        //Find the admin user token, based on tokenid
        if (!userIdentityService.allowedToUpdate(applicationtokenid, adminUserTokenId)) {
            String adminUserName = userIdentityService.findUserByTokenId(adminUserTokenId);
            log.info("Not allowed to update password. adminUser {}, user to update {}", adminUserName, username);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);

            if (user == null) {
                log.trace("No user found for username {}, can not update password.", username);
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            log.debug("Found user: {}", user.toString());

            userIdentityService.changePassword(username, user.getUid(), password);
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePasswordForUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /*
    @PUT
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUserIdentityForUsername(@PathParam("username") String username, String userIdentityJson) {
        log.trace("updateUserIdentityForUsername: username={}, userJson={}", username, userIdentityJson);

        try {
            UserIdentityRepresentation newUserIdentity = userAggregateService.updateUserIdentityForUsername(username, userIdentityJson);
            return Response.ok().build();   //TODO return whydahUserIdentity
        } catch (IllegalArgumentException iae) {
            log.error("updateUserIdentityForUsername: Invalid json={}", userIdentityJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    */

    /*
    @GET
    @Path("/{uid}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUserIdentity(@PathParam("uid") String uid) {
        log.trace("getUserIdentity, uid={}", uid);

        UserAggregate userAggregate = userAggregateService.getUserAggregateForUid(uid);

        String json;
        try {
            json = mapper.writeValueAsString(userAggregate);
        } catch (IOException e) {
            log.error("Error converting to json. {}", userAggregate.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(json).build();
    }
    */



    /*
     * Add user from json
     * Add default roles to new user
     *
     * @param userIdentityJson  json representing a UserIdentity
     * @return  UserAggregate with default roles
     */
    /*
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUserAndSetDefaultRoles(String userIdentityJson) {
        log.trace("addUserAndSetDefaultRoles is called with userIdentityJson={}", userIdentityJson);
        try {
            UserAggregate userAggregate = userAggregateService.addUserAndSetDefaultRoles(userIdentityJson);
            return Response.status(Response.Status.CREATED).build();   //TODO return userAggregate or UserIdentity?
        } catch (IllegalArgumentException iae) {
            log.error("addUserAndSetDefaultRoles: Invalid json={}", userIdentityJson, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage());
            return Response.status(Response.Status.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    */


    /*
    @PUT
    @Path("/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUserAggregate(@PathParam("username") String username, String json) {
        log.trace("updateUserAggregate: username={}, json={}", username, json);

        UserAggregate request;
        try {
            request = objectMapper.readValue(json, UserAggregate.class);
        } catch (IOException e) {
            log.error("Bad json: " + json, e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            //UserIdentity newUserIdentity = userAggregateService.updateUserIdentityForUsername(username, userIdentityJson);
            //return Response.ok().build();   //TODO return whydahUserIdentity

            UserAggregate userAggregate = userAggregateService.updateUserAggregate(username, request);

            Writer strWriter = new StringWriter();
            try {
                objectMapper.writeValue(strWriter, userAggregate);
            } catch (IOException e) {
                log.error("Could not convert to JSON {}", userAggregate.toString(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error converting to requested format.").build();
            }
            return Response.ok(strWriter.toString()).build();
        } catch (IllegalArgumentException iae) {
            log.error("updateUserAggregate: Invalid json={}", json, iae);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
     */

    /*
    @DELETE
    @Path("/{username}")
    public Response deleteUserAggregate(@PathParam("username") String username) {
        try {
            userAggregateService.deleteUserAggregateByUsername(username);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (IllegalArgumentException iae) {
            log.error("deleteUserIdentity failed username={}", username + ". " + iae.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    */


    /*
    @POST
    @Path("users/{username}/newuser/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Endrer data for ny bruker {}: {}", username, passwordJson);
        try {
            UserIdentity user = userIdentityService.getUserIndentity(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userIdentityService.authenticateWithChangePasswordToken(username, token);
            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                String newusername = jsonobj.getString("newusername");
                if (!username.equals(newusername)) {
                    UserIdentity newidexists = userIdentityService.getUserIndentity(newusername);
                    if (newidexists != null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build();
                    }
                    user.setUsername(newusername);
                    userIdentityService.updateUserIdentityForUsername(username, user);
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


    /*
     * Lister alle applikasjoner, samt angir om brukeren har noen roller her.
     *
     * @param username user id
     * @return app-liste.
     */
    /*
    @GET
    @Path("/{username}/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersApplications(@PathParam("username") String username) {
        UserAggregate userAggregate;
        try {
            userAggregate = userAggregateService.getUserAggregateByUsername(username);
            if (userAggregate == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        List<Application> allApps = applicationRepository.getApplications();
        Set<String> myApps = new HashSet<>();
        for (UserPropertyAndRole role : userAggregate.getRoles()) {
            myApps.add(role.getApplicationId());
        }

        HashMap<String, Object> model = new HashMap<>(3);
        model.put("allApps", allApps);
        model.put("myApps", myApps);
        return Response.ok(new Viewable("/useradmin/userapps.json.ftl", model)).build();
    }
    */


    /////////// Roles

    /*
    @GET
    @Path("users/{username}/{appid}/deleteall")
    public Response deleteAllUserRolesForApp(@PathParam("username") String username, @PathParam("appid") String appid) {
        log.debug("Fjern alle roller for {}: {}", username, appid);
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
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
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
        } catch (NamingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if (whydahUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        UserAggregate whydahUser = new UserAggregate(whydahUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid()));
        List<UserPropertyAndRole> rolesForApp = new ArrayList<>();
        for (UserPropertyAndRole role : whydahUser.getPropsAndRoles()) {
            if (role.getApplicationId().equals(appid)) {
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
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
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
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
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
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
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
        role.setApplicationId(appid);
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            role.setOrganizationId(jsonobj.getString("orgID"));
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
            if (existingRole.getApplicationId().equals(appid) && existingRole.getOrganizationId().equals(role.getOrganizationId()) && existingRole.getRoleName().equals(role.getRoleName())) {
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
        UserIdentity whydahUserIdentity;
        try {
            whydahUserIdentity = userIdentityService.getUserIndentity(username);
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
        String orgName = userPropertyAndRoleRepository.getOrgname(app.getDefaultOrgName());
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(whydahUserIdentity.getUid());
        role.setApplicationId(appid);
        role.setApplicationName(app.getName());
        role.setOrganizationId(app.getDefaultOrgName());
        role.setOrganizationName(orgName);
        role.setRoleName(app.getDefaultrole());
        log.debug("Role: {}", role);
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(whydahUserIdentity.getUid());
        for (UserPropertyAndRole existingRole : existingRoles) {
            if (existingRole.getApplicationId().equals(appid) && existingRole.getOrganizationId().equals(role.getOrganizationId()) && existingRole.getRoleName().equals(role.getRoleName())) {
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
