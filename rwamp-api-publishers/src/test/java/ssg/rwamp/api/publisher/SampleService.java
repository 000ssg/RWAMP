package ssg.rwamp.api.publisher;

import ssg.rwamp.api.provider.annotations.*;

import java.util.List;

@ApiService(name = "users", path = "/api/users", tags = {"admin"})
public class SampleService {
    @Operation(summary = "Get user", httpMethods = {HttpMethod.GET})
    public String getUser(@ApiParam(name = "id") Long id) { return "user-" + id; }

    @Operation(summary = "List users", httpMethods = {HttpMethod.GET})
    public List<String> listUsers() { return List.of("alice", "bob"); }
}
