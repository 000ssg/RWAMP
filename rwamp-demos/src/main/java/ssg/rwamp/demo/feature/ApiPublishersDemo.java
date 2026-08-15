package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.AnnotationBasedApiProvider;
import ssg.rwamp.api.provider.annotations.*;
import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.publisher.UnifiedApiPublisher;
import ssg.rwamp.api.publisher.wamp.WampApiPublisher;

import java.util.List;
import java.util.Map;

/**
 * Demo of API publishing — exposing APIs as OpenAPI specs, WAMP procedures, and HTML docs.
 * <p>
 * This demo:
 * <ol>
 *   <li>Discovers API from annotated service classes</li>
 *   <li>Generates OpenAPI 3.1.x JSON spec</li>
 *   <li>Registers operations as WAMP meta-procedures</li>
 *   <li>Generates interactive HTML documentation page</li>
 * </ol>
 *
 * @since 0.1.0
 */
public class ApiPublishersDemo {

    @ApiService(name = "greeting", description = "Greeting service",
            path = "/api/greeting", tags = {"basic"})
    public static class GreetingService {
        @Operation(summary = "Say hello", description = "Returns a greeting message",
                httpMethods = {HttpMethod.GET})
        public String hello(@ApiParam(name = "name", description = "Person to greet") String name) {
            return "Hello, " + name + "!";
        }

        @Operation(summary = "Goodbye", httpMethods = {HttpMethod.GET})
        public String goodbye(@ApiParam(name = "name") String name) {
            return "Goodbye, " + name + "!";
        }
    }

    @ApiService(name = "calculator", path = "/api/calc", tags = {"math"})
    public static class CalculatorService {
        @Operation(summary = "Add two numbers", httpMethods = {HttpMethod.POST})
        public double add(@ApiParam(name = "a") double a, @ApiParam(name = "b") double b) {
            return a + b;
        }

        @Operation(summary = "Multiply", httpMethods = {HttpMethod.POST})
        public double multiply(@ApiParam(name = "a") double a,
                                @ApiParam(name = "b") double b) {
            return a * b;
        }
    }

    public record DemoResult(String openApiSnippet,
                              Map<String, String> wampUris,
                              String htmlSnippet) {}

    public DemoResult run() {
        var router = new WampRouter();

        var result = new UnifiedApiPublisher("demo-api")
                .version("1.0.0")
                .provider(new AnnotationBasedApiProvider("demo-api"))
                .openApiPublisher(p -> p
                    .serverUrl("http://localhost:8080")
                    .contact("Demo Team", "demo@example.com")
                    .license("MIT"))
                .wampPublisher(router, "com.demo")
                .jsDocPublisher(p -> p.title("Demo API Documentation"))
                .publish(GreetingService.class, CalculatorService.class);

        String openApiSnippet = result.openApiJson() != null
                ? result.openApiJson().substring(0, Math.min(200, result.openApiJson().length())) + "..."
                : "null";

        String htmlSnippet = result.jsDocHtml() != null
                ? result.jsDocHtml().substring(0, Math.min(200, result.jsDocHtml().length())) + "..."
                : "null";

        return new DemoResult(openApiSnippet, result.wampResults(), htmlSnippet);
    }

    public static void main(String[] args) {
        var demo = new ApiPublishersDemo();
        var result = demo.run();
        System.out.println("ApiPublishersDemo:");
        System.out.println("  OpenAPI spec: " + result.openApiSnippet());
        System.out.println("  WAMP URIs: " + result.wampUris());
        System.out.println("  HTML doc: " + result.htmlSnippet());
    }
}
