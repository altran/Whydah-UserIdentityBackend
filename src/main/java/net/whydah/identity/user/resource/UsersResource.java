package net.whydah.identity.user.resource;


import com.fasterxml.jackson.core.JsonProcessingException;
import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.UIBUserIdentity;
import net.whydah.identity.user.identity.UIBUserIdentityRepresentation;
import net.whydah.identity.user.search.PaginatedUIBUserAggregateDataList;
import net.whydah.identity.user.search.PaginatedUIBUserIdentityDataList;
import net.whydah.identity.user.search.UserSearch;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Endpoint for collection of users.
 */
@Component
@Path("/{applicationtokenid}/{usertokenid}/users")
public class UsersResource {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);
    private final UserSearch userSearch;
    private final UserAggregateService userAggregateService; //to help for retrieving roles for users. This is used for export feature

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UsersResource(UserSearch userSearch, UserAggregateService userAggregateService) {
        this.userSearch = userSearch;
        this.userAggregateService = userAggregateService;
    }

    /**
     * Find users.
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findUsers(@PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        List<UserIdentity> users = userSearch.search(query);
        HashMap<String, Object> model = new HashMap<>(2);
        model.put("users", users);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", users.size());
        Response response = Response.ok(new Viewable("/useradmin/users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }

    /**
     * Lookup users in directory  (Intermediate copy of find)
     * @param query User query.
     * @return json response.
     */
    @GET
    @Path("/search/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response userSearch(@PathParam("q") String query) {
        return findUsers(query);
    }
    
    //NEW ONE ADDED FOR PAGINATED SEARCHING (WITHOUT ROLES INCLUDED)
    @GET
    @Path("/query/{p}/{q}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response queryUsers(@PathParam("p") String pageNumber, @PathParam("q") String query) {
        log.trace("findUsers with query=" + query);
        PaginatedUIBUserIdentityDataList dl = userSearch.query(Integer.valueOf(pageNumber) , query);
        HashMap<String, Object> model = new HashMap<>();
        model.put("currentPage", String.valueOf(dl.pageNumber));
        model.put("pageSize", String.valueOf(dl.pageSize));
        model.put("totalItems", String.valueOf(dl.totalCount));
        model.put("users", dl.data);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", dl.data.size());
        Response response = Response.ok(new Viewable("/useradmin/paginated_users.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }
    
    //NEW ONE ADDED FOR PAGINATED EXPORT (WITH ROLES INCLUDED)
    @GET
    @Path("/export/{p}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response exportUsers(@PathParam("p") String pageNumber) {
        log.trace("exportUsers with for the page =" + pageNumber);
        PaginatedUIBUserIdentityDataList dl = userSearch.query(Integer.valueOf(pageNumber) , "*");
        PaginatedUIBUserAggregateDataList adl = new PaginatedUIBUserAggregateDataList(dl.pageNumber, dl.totalCount, new ArrayList<UIBUserAggregateRepresentation>());
        for(UIBUserIdentityRepresentation ui : dl.data){
        	adl.data.add(UIBUserAggregateRepresentation.fromUserAggregate(new UIBUserAggregate((UIBUserIdentity) ui, userAggregateService.getRoles(ui.getUid()))));
        }
        
        HashMap<String, Object> model = new HashMap<>();
        model.put("currentPage", String.valueOf(adl.pageNumber));
        model.put("pageSize", String.valueOf(adl.pageSize));
        model.put("totalItems", String.valueOf(adl.totalCount));
        model.put("users", adl.data);
        model.put("userbaseurl", uriInfo.getBaseUri());
        log.trace("findUsers returned {} users.", adl.data.size());
        Response response = Response.ok(new Viewable("/useradmin/paginated_exportedusers.json.ftl", model)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=utf-8").build();
        return response;
    }
    
    //NEW ONE ADDED FOR CHECKING DUPLICATE USERS BEFORE IMPORTiNG
    @POST
    @Path("/checkduplicates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDuplicateUsers(String json) throws JsonProcessingException, IOException {
        log.trace("getDuplicateUsers");
        String resData = "";
        //parse json
        List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
        List<String> duplicates = new ArrayList<String>();
        for(UserAggregate ua : importList){
        	
        	UIBUserIdentity uibua = userAggregateService.getUIBUserIdentityByUsernameOrUid(ua.getUsername());
        	if(uibua!=null){
        		duplicates.add(UserIdentityMapper.toJson(ua));
        	}
        }
        return Response.ok("[" + (duplicates.size()>0 ? StringUtils.join(duplicates, ','):"") + "]").build();
    }
    
}
