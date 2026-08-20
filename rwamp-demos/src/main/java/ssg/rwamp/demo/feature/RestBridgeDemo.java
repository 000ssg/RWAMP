package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.session.SessionMetaApi;
import ssg.rwamp.feature.session.SessionTransportTracker;
import ssg.rwamp.feature.virtual.VirtualSessionApi;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestWampBridge;

import java.util.List;
import java.util.Map;

/**
 * REST bridge demo — invoking WAMP procedures and publishing topics via HTTP.
 * <p>
 * Scenario: A WAMP router has procedures registered. HTTP clients call these
 * procedures through the REST bridge, which creates virtual sessions for each
 * caller and routes calls through the WAMP router. The same procedure can be
 * invoked both via WAMP (WebSocket) and via REST (HTTP).
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>REST-to-WAMP call bridging — HTTP GET/POST mapped to WAMP CALL</li>
 *   <li>REST-to-WAMP publish bridging — HTTP POST mapped to WAMP PUBLISH</li>
 *   <li>Virtual session management — one session per auth identity, reused across requests</li>
 *   <li>Path-to-URI resolution — "/com/example/add" maps to "com.example.add"</li>
 *   <li>Session cleanup — removing virtual sessions when auth tokens expire</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Unified API — same business logic serves both WAMP and HTTP clients</li>
 *   <li>Low latency for simple calls — direct routing without serialization overhead</li>
 *   <li>HTTP auth integration — authid from HTTP headers maps to WAMP session</li>
 *   <li>Fire-and-forget pub/sub — HTTP clients can trigger WAMP events</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>No bidirectional communication — HTTP is request/response only</li>
 *   <li>Virtual sessions accumulate — need cleanup strategy for anonymous callers</li>
 *   <li>No streaming — cannot use progressive results or pub/sub subscriptions</li>
 *   <li>Authentication gap — REST bridge uses simple authid, not full WAMP auth</li>
 *   <li>Path convention — slashes map to dots, which may collide with real URIs</li>
 * </ul>
 * <p>
 * Resource usage per virtual session: ~200 bytes (WampSession). Each REST caller
 * gets one session, reused for subsequent requests. Total memory for 1000 concurrent
 * HTTP users: ~200KB for sessions alone.
 *
 * @since 0.1.0
 */
public class RestBridgeDemo {

    /**
     * Result of a REST bridge call.
     *
     * @param path       the request path
     * @param statusCode HTTP status code
     * @param body       the response body
     */
    public record CallResult(String path, int statusCode, Object body) {}

    /**
     * Full demo result.
     *
     * @param wampCall      the WAMP call result
     * @param restCall      the REST bridge call result
     * @param restPublish   the REST bridge publish result
     * @param virtualSessions number of virtual sessions created
     */
    public record DemoResult(CallResult wampCall,
                              CallResult restCall,
                              CallResult restPublish,
                              int virtualSessions) {}

    private final WampRouter router = new WampRouter();
    private final Realm realm = new Realm("realm1");
    private final VirtualSessionManager virtualManager = new VirtualSessionManager(realm);

    /**
     * Runs the demo: registers procedures, calls via WAMP and REST, publishes via REST.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Register all RWAMP features
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);
        VirtualSessionApi.register(router, realm, virtualManager);
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // Register sample procedures
        router.registerMetaProcedure("com.example.greet", (call, transport) -> {
            String name = call.args() != null && !call.args().isEmpty()
                    ? call.args().get(0).toString() : "World";
            return List.of("Hello, " + name + "!");
        });

        router.registerMetaProcedure("com.example.double", (call, transport) -> {
            int val = call.args() != null && !call.args().isEmpty()
                    ? ((Number) call.args().get(0)).intValue() : 0;
            return List.of(val * 2);
        });

        // Create the REST bridge
        var bridge = new RestWampBridge(router, virtualManager,
                ReflectionApi.createRegistry(router));

        // 1. Call via WAMP (direct)
        var wampResult = callViaWamp(router);

        // 2. Call via REST bridge
        var restRequest = new RestRequest("GET", "/com/example/greet",
                List.of("Alice"), Map.of("authid", "http-user-1"), null);
        var restResponse = bridge.handle(restRequest);
        var restResult = new CallResult("/com/example/greet",
                restResponse.statusCode(), restResponse.body());

        // 3. Publish via REST bridge
        var publishRequest = new RestRequest("POST", "/events/user/login",
                List.of("user1"), Map.of("authid", "http-user-2"), null);
        var publishResponse = bridge.publish(publishRequest);
        var publishResult = new CallResult("/events/user/login",
                publishResponse.statusCode(), publishResponse.body());

        // 4. Check virtual sessions
        int virtualSessions = virtualManager.getCount();

        return new DemoResult(wampResult, restResult, publishResult, virtualSessions);
    }

    private CallResult callViaWamp(WampRouter router) {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, Map.of("_caller_session", 100L),
                "com.example.greet", List.of("Bob"));
        pair[0].send(call);
        var received = (WampMessage.Call) pair[1].receive();
        router.route(received, pair[0], 100L);
        var result = (WampMessage.Result) pair[0].receive();
        return new CallResult("com.example.greet", 200, result.args());
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new RestBridgeDemo();
        var result = demo.run();
        System.out.println("RestBridgeDemo:");
        System.out.println("  WAMP call: " + result.wampCall().body());
        System.out.println("  REST call: " + result.restCall().statusCode() + " -> " + result.restCall().body());
        System.out.println("  REST publish: " + result.restPublish().statusCode() + " -> " + result.restPublish().body());
        System.out.println("  Virtual sessions: " + result.virtualSessions());
    }
}
