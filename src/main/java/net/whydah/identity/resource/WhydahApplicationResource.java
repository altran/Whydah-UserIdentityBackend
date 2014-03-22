package net.whydah.identity.resource;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Created by baardl on 22.03.14.
 */
public class WhydahApplicationResource {

    //TODO baardl preparing for Application Authorization.

    Response info() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("<error>Server error, not implemented.</error>").build();
    }

    Response authenticateApplication(InputStream input){
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("<error>Server error, not implemented.</error>").build();
    }

    Response createApplication(InputStream input){
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("<error>Server error, not implemented.</error>").build();

    }
}
