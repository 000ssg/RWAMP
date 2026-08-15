package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.*;
import ssg.rwamp.api.provider.annotations.*;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.UnifiedApiPublisher;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestResponse;

import java.util.List;
import java.util.Map;

/**
 * Full pipeline demo: annotation → provider → publishers → multiple outputs.
 * <p>
 * Shows how to:
 * <ol>
 *   <li>Define API services with annotations</li>
 *   <li>Discover APIs using AnnotationBasedApiProvider</li>
 *   <li>Generate OpenAPI specs (JSON and YAML)</li>
 *   <li>Register WAMP procedures</li>
 *   <li>Expose REST HTTP endpoints</li>
 *   <li>Generate interactive HTML documentation</li>
 * </ol>
 *
 * @since 0.1.0
 */
public class FullPipelineDemo {

    // ── Sample API Services ──

    @ApiService(name = "users", description = "User management",
            path = "/api/users", tags = {"users", "crud"})
    public static class UserService {
        @Operation(summary = "Get user by ID", description = "Returns a user by unique identifier",
                operationId = "getUserById", httpMethods = {HttpMethod.GET})
        public UserEntity getUser(@ApiParam(name = "id", description = "User ID") Long id) {
            return new UserEntity(id, "alice", "alice@example.com");
        }

        @Operation(summary = "List users", description = "Returns paginated list of users",
                httpMethods = {HttpMethod.GET})
        public List<UserEntity> listUsers(
                @ApiParam(required = false, defaultValue = "0") int page,
                @ApiParam(required = false, defaultValue = "20") int size) {
            return List.of(new UserEntity(1L, "alice", "alice@example.com"));
        }

        @Operation(summary = "Create user", description = "Creates a new user account",
                httpMethods = {HttpMethod.POST})
        public UserEntity createUser(
                @ApiParam(name = "name") String name,
                @ApiParam(name = "email") String email) {
            return new UserEntity(System.nanoTime(), name, email);
        }
    }

    @ApiService(name = "orders", description = "Order processing",
            path = "/api/orders", tags = {"orders", "ecommerce"})
    public static class OrderService {
        @Operation(summary = "Get order", httpMethods = {HttpMethod.GET})
        public OrderEntity getOrder(@ApiParam(name = "orderId") Long orderId) {
            return new OrderEntity(orderId, "processing", 2);
        }

        @Operation(summary = "Cancel order", httpMethods = {HttpMethod.POST})
        public boolean cancelOrder(@ApiParam(name = "orderId") Long orderId) {
            return true;
        }
    }

    public record UserEntity(Long id, String name, String email) {}
    public record OrderEntity(Long orderId, String status, int itemCount) {}

    // ── Demo Result ──

    public record DemoResult(
            String openApiJsonPreview,
            String openApiYamlPreview,
            Map<String, String> wampUris,
            Map<String, String> restPaths,
            String htmlPreview,
            RestResponse sampleResponse) {}

    // ── Main Demo ──

    public DemoResult run() {
        var router = new WampRouter();
        var realm = new Realm("default");
        var sessionManager = new VirtualSessionManager(realm);

        // Unified publishing pipeline
        var result = new UnifiedApiPublisher("sample-api")
                .version("1.0.0")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .openApiPublisher(p -> p
                    .serverUrl("https://api.example.com")
                    .contact("Demo Team", "demo@example.com")
                    .license("MIT"))
                .wampPublisher(router, "com.demo")
                .restPublisher(router, sessionManager, "com.demo", p -> p.basePath("/api"))
                .jsDocPublisher(p -> p.title("Sample API Documentation"))
                .publish(UserService.class, OrderService.class);

        // Preview OpenAPI JSON
        String openApiJsonPreview = result.openApiJson() != null
                ? result.openApiJson().substring(0, Math.min(300, result.openApiJson().length())) + "..."
                : "null";

        // Preview OpenAPI YAML
        String openApiYamlPreview = result.openApiYaml() != null
                ? result.openApiYaml().substring(0, Math.min(300, result.openApiYaml().length())) + "..."
                : "null";

        // WAMP URIs
        Map<String, String> wampUris = result.wampResults() != null ? result.wampResults() : Map.of();

        // REST paths
        Map<String, String> restPaths = result.restPathMappings() != null ? result.restPathMappings() : Map.of();

        // HTML preview
        String htmlPreview = result.jsDocHtml() != null
                ? result.jsDocHtml().substring(0, Math.min(200, result.jsDocHtml().length())) + "..."
                : "null";

        // Try a REST request
        RestResponse sampleResponse = null;
        if (result.restBridge() != null) {
            sampleResponse = result.restBridge().handle(
                    new RestRequest("GET", "/api/users/123", List.of(), Map.of(), null));
        }

        return new DemoResult(openApiJsonPreview, openApiYamlPreview, wampUris, restPaths, htmlPreview, sampleResponse);
    }

    public static void main(String[] args) {
        var demo = new FullPipelineDemo();
        var result = demo.run();

        System.out.println("=== Full Pipeline Demo ===\n");

        System.out.println("OpenAPI JSON:");
        System.out.println(result.openApiJsonPreview());
        System.out.println();

        System.out.println("OpenAPI YAML:");
        System.out.println(result.openApiYamlPreview());
        System.out.println();

        System.out.println("WAMP Procedure URIs:");
        for (var entry : result.wampUris().entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }
        System.out.println();

        System.out.println("REST Path Mappings:");
        for (var entry : result.restPaths().entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }
        System.out.println();

        System.out.println("HTML Doc Preview:");
        System.out.println(result.htmlPreview());
        System.out.println();

        if (result.sampleResponse() != null) {
            System.out.println("Sample REST Response:");
            System.out.println("  status: " + result.sampleResponse().statusCode());
            System.out.println("  body: " + result.sampleResponse().body());
        }

        System.out.println("\n=== Demo complete ===");
    }
}
