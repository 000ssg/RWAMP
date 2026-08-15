package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Sample service for testing annotation-based API discovery.
 */
@ApiService(name = "users", description = "User management API",
        path = "/api/users", tags = {"admin", "crud"})
public class SampleService {

    @Operation(summary = "Get user by ID", description = "Returns a user by unique identifier",
            operationId = "getUserById", httpMethods = {HttpMethod.GET}, tags = {"read"})
    public UserEntity getUser(@ApiParam(name = "userId", required = true, description = "User unique ID") Long id) {
        return null;
    }

    @Operation(summary = "List users", httpMethods = {HttpMethod.GET})
    public List<UserEntity> listUsers(@ApiParam(required = false, defaultValue = "0") int page,
                                       @ApiParam(required = false, defaultValue = "20") int size) {
        return null;
    }

    @Operation(summary = "Create user", httpMethods = {HttpMethod.POST})
    public UserEntity createUser(@ApiParam(name = "name") String name,
                                  @ApiParam(name = "email") String email) {
        return null;
    }

    @Operation(exclude = true)
    public void internalMethod() {
    }

    @ApiIgnore
    public void ignoredMethod() {
    }

    public String getStatus() {
        return "ok";
    }

    public void setDescription(String desc) {
    }

    // Not public — should be ignored
    String privateMethod() { return null; }
}
