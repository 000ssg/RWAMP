package ssg.rwamp.demo.advanced;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestResponse;
import ssg.rwamp.rest.RestWampBridge;

import java.util.List;
import java.util.Map;

/**
 * Multi-version API demo — publishing multiple API versions on WAMP and REST.
 * <p>
 * Scenario: A service publishes two versions of the same API (v1 and v2) on WAMP.
 * The REST bridge exposes both versions. v1 returns a simple greeting, v2 returns
 * a structured response with metadata.
 * <p>
 * Procedure URIs:
 * <ul>
 *   <li>{@code com.app.greet.v1} — simple greeting: returns ["Hello, Name!"]</li>
 *   <li>{@code com.app.greet.v2} — structured: returns [{"name":"Name","version":2,"timestamp":...}]</li>
 * </ul>
 * REST paths:
 * <ul>
 *   <li>{@code /com/app/greet/v1} → calls com.app.greet.v1</li>
 *   <li>{@code /com/app/greet/v2} → calls com.app.greet.v2</li>
 * </ul>
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>API versioning via procedure URI conventions (namespace.version)</li>
 *   <li>Parallel operation — both versions coexist on the same router</li>
 *   <li>REST bridge versioning — path segments map to URI segments</li>
 *   <li>Backward compatibility — v1 clients continue to work while v2 is available</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Zero-downtime migration — v2 can be deployed alongside v1</li>
 *   <li>Gradual adoption — clients migrate at their own pace</li>
 *   <li>Version-specific tuning — v2 can use more efficient serialization</li>
 *   <li>Clear deprecation path — v1 can be marked for removal</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Code duplication — v1 and v2 handlers are separate implementations</li>
 *   <li>Router namespace pollution — each version adds procedure registrations</li>
 *   <li>Testing matrix — must test each version independently</li>
 *   <li>Resource overhead — each version consumes memory for its handler</li>
 *   <li>Version lifecycle management — deciding when to sunset v1</li>
 * </ul>
 * <p>
 * Resource comparison:
 * <ul>
 *   <li>Per-version overhead: ~1KB per registered procedure (Dealer state)</li>
 *   <li>REST bridge shares the same virtual sessions across versions</li>
 *   <li>No additional router instances needed</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class MultiVersionApiDemo {

    /**
     * Result of calling a specific API version.
     *
     * @param version    the API version
     * @param transport  how the call was made (WAMP or REST)
     * @param request    the request details
     * @param response   the response data
     * @param statusCode HTTP status (only for REST)
     */
    public record VersionCallResult(int version, String transport,
                                     String request, Object response, int statusCode) {}

    /**
     * Full demo result.
     *
     * @param results list of call results for each version/transport combination
     */
    public record DemoResult(List<VersionCallResult> results) {}

    private final WampRouter router = new WampRouter();
    private final Realm realm = new Realm("realm1");
    private final VirtualSessionManager virtualManager = new VirtualSessionManager(realm);

    /**
     * Runs the demo: calls v1 and v2 via both WAMP and REST.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Set up REST bridge
        var bridge = new RestWampBridge(router, virtualManager,
                ReflectionApi.createRegistry(router));

        // Register v1 procedure
        router.registerMetaProcedure("com.app.greet.v1", (call, transport) -> {
            String name = call.args() != null && !call.args().isEmpty()
                    ? call.args().get(0).toString() : "World";
            return List.of("Hello, " + name + "!");
        });

        // Register v2 procedure
        router.registerMetaProcedure("com.app.greet.v2", (call, transport) -> {
            String name = call.args() != null && !call.args().isEmpty()
                    ? call.args().get(0).toString() : "World";
            return List.of(Map.of(
                    "name", name,
                    "greeting", "Hello, " + name + "!",
                    "version", 2,
                    "timestamp", System.currentTimeMillis()
            ));
        });

        var results = new java.util.ArrayList<VersionCallResult>();

        // 1. Call v1 via WAMP
        var v1Wamp = callViaWamp("com.app.greet.v1", List.of("Alice"));
        results.add(new VersionCallResult(1, "WAMP", "com.app.greet.v1(\"Alice\")", v1Wamp, 0));

        // 2. Call v2 via WAMP
        var v2Wamp = callViaWamp("com.app.greet.v2", List.of("Alice"));
        results.add(new VersionCallResult(2, "WAMP", "com.app.greet.v2(\"Alice\")", v2Wamp, 0));

        // 3. Call v1 via REST
        var restV1 = bridge.handle(new RestRequest("GET", "/com/app/greet/v1",
                List.of("Alice"), Map.of("authid", "api-user"), null));
        results.add(new VersionCallResult(1, "REST", "GET /com/app/greet/v1?name=Alice",
                restV1.body(), restV1.statusCode()));

        // 4. Call v2 via REST
        var restV2 = bridge.handle(new RestRequest("GET", "/com/app/greet/v2",
                List.of("Alice"), Map.of("authid", "api-user"), null));
        results.add(new VersionCallResult(2, "REST", "GET /com/app/greet/v2?name=Alice",
                restV2.body(), restV2.statusCode()));

        return new DemoResult(List.copyOf(results));
    }

    private Object callViaWamp(String procedure, List<Object> args) {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, Map.of("_caller_session", 100L),
                procedure, args);
        pair[0].send(call);
        var received = (WampMessage.Call) pair[1].receive();
        router.route(received, pair[0], 100L);
        var result = (WampMessage.Result) pair[0].receive();
        return result.args();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new MultiVersionApiDemo();
        var result = demo.run();
        System.out.println("MultiVersionApiDemo — API versioning comparison:");
        for (var r : result.results()) {
            System.out.println("  v" + r.version() + " (" + r.transport() + "): " +
                    r.request() + " -> " + r.response());
        }
    }
}
