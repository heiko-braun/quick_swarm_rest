package io.obsidian.quickstart.swarm.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author Heiko Braun
 * @since 04/10/16
 */
@Path("/service")
@Api(value = "/service", description = "Available REST services", tags = "demo")
public class RestEndpoint {

    @GET
    @Path("/say/{name}")
    @ApiOperation(value = "Respond to request",
            notes = "Returns the response as a string",
            response = String.class
    )
    public String say(@ApiParam("name") @PathParam("name") String name) {
        return "Hello from REST endpoint to "+name;
    }
}
