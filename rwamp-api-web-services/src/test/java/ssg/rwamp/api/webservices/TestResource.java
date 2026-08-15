package ssg.rwamp.api.webservices;

import ssg.rwamp.api.webservices.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * Sample JAX-RS-like resource for testing.
 */
@TestPath("/api/users")
@TestConsumes({"application/json"})
@TestProduces({"application/json"})
public class TestResource {

    @TestGet
    @TestPath("/list")
    @TestProduces({"application/json"})
    public List<String> listUsers() {
        return List.of("alice", "bob");
    }

    @TestGet
    @TestPath("/{id}")
    public String getUser(@TestPathParam("id") String userId) {
        return "user:" + userId;
    }

    @TestGet
    @TestPath("/search")
    public List<String> searchUsers(
            @TestQueryParam("q") String query,
            @TestQueryParam(value = "limit", defaultValue = "10") int limit) {
        return List.of();
    }

    @TestPost
    @TestPath("/create")
    @TestConsumes({"application/json"})
    public String createUser(@TestQueryParam("name") String name) {
        return "created:" + name;
    }

    @TestPost
    public String fallbackMethod(String body) {
        return "fallback:" + body;
    }

    // This should be skipped (no method annotation and not by convention)
    public void internalHelper() {
    }

    // Java.lang.Object methods should be skipped
    @Override
    public String toString() {
        return "TestResource";
    }
}
