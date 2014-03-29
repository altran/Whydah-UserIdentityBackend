package net.whydah.identity.application;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Created by baardl on 29.03.14.
 */
public class ApplicationResource {

    //TODO move to ApplicationResource
    Response createApplication(InputStream input){
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("<error>Server error, not implemented.</error>").build();

    }
}
