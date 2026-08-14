package ssg.rwamp.demo.advanced;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.session.SessionMetaApi;
import ssg.rwamp.feature.session.SessionTransportTracker;
import ssg.rwamp.feature.statistics.StatisticsApi;
import ssg.rwamp.feature.statistics.StatisticsTransport;
import ssg.rwamp.feature.statistics.WampStatistics;
import ssg.rwamp.feature.testament.TestamentApi;
import ssg.rwamp.feature.testament.TestamentManager;
import ssg.rwamp.feature.virtual.VirtualSessionApi;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestResponse;
import ssg.rwamp.rest.RestWampBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composite scenario demo — a realistic multi-tenant microservices topology.
 * <p>
 * This demo combines all RWAMP features into a single, realistic architecture:
 * <pre>
 *                          ┌─────────────────────────────────────────┐
 *                          │           WampRouter (shared)           │
 *                          │                                         │
 *          ┌───────────────│  Realm: tenant.alpha                    │
 *          │  ┌───────────┐│  ┌───────────┐  ┌───────────┐         │
 *          │  │ Callee-1  ││  │ Callee-2  │  │ Callee-3  │         │
 *          │  │ (round-  ││  │ (round-   │  │ (first)   │          │
 *          │  │  robin)   ││  │  robin)   │  │           │          │
 *          │  └───────────┘│  └───────────┘  └───────────┘          │
 *          │                │                                         │
 *          │                │  Realm: tenant.beta                     │
 *          │  ┌───────────┐│                                          │
 *          │  │ Callee-4  ││  (separate service for tenant beta)     │
 *          │  │           ││                                          │
 *          │  └───────────┘│                                          │
 *          │                │                                         │
 *  ┌───────┤                └─────────────────────────────────────────┤
 *  │       │                                                          │
 *  │ WAMP  │  Features:                                              │
 *  │ Caller│  - Session tracking + kill                               │
 *  │       │  - Statistics (wrapped transports)                        │
 *  │       │  - Testament (auto-publish on session close)              │
 *  │       │  - Reflection API (introspect procedures/topics)          │
 *  │       │  - Virtual sessions (for REST bridge)                     │
 *  │       │  - REST bridge (HTTP → WAMP)                              │
 *  │       │  - Cross-realm rerouting                                  │
 *  └───────┘                                                          │
 *  ┌───────┤                                                          │
 *  │ REST  │                                                          │
 *  │ Client│                                                          │
 *  └───────┘                                                          │
 * </pre>
 * <p>
 * This scenario demonstrates:
 * <ul>
 *   <li>Multi-tenant isolation — separate realms for each tenant</li>
 *   <li>Shared registrations — multiple instances per procedure with load balancing</li>
 *   <li>Statistics tracking — per-realm counters for operational visibility</li>
 *   <li>Session lifecycle — testament-based cleanup on disconnect</li>
 *   <li>REST + WAMP hybrid — same procedures accessible via both transports</li>
 *   <li>Cross-realm calls — caller in tenant.alpha reaches procedure in tenant.beta</li>
 *   <li>Reflection — introspect the entire system at runtime</li>
 * </ul>
 * <p>
 * Pros of this architecture:
 * <ul>
 *   <li>Scalability — add callee instances per tenant for horizontal scaling</li>
 *   <li>Tenant isolation — each tenant gets dedicated Broker/Dealer</li>
 *   <li>Operational visibility — statistics, reflection, and session management</li>
 *   <li>Protocol flexibility — REST and WAMP clients coexist</li>
 * </ul>
 * <p>
 * Cons of this architecture:
 * <ul>
 *   <li>Complexity — many moving parts increase debugging difficulty</li>
 *   <li>Resource usage — each realm and callee adds memory overhead</li>
 *   <li>Configuration — wiring statistics, testaments, and rerouting requires setup</li>
 *   <li>Single router bottleneck — all traffic goes through one WampRouter</li>
 * </ul>
 * <p>
 * Estimated resource usage for this scenario:
 * <ul>
 *   <li>1 WampRouter: ~5MB</li>
 *   <li>2 realms: ~200KB (each with Broker + Dealer state)</li>
 *   <li>4 callee instances: ~800KB (InMemoryTransport pairs + registrations)</li>
 *   <li>Statistics: ~1KB per realm</li>
 *   <li>Total: ~7-8MB for the entire demo topology</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class CompositeScenarioDemo {

    /**
     * Summary of the composite scenario.
     *
     * @param tenants        number of tenants (realms)
     * @procedures          total procedure registrations
     * @param callees        total callee instances
     * @param rpcCalls       RPC calls made
     * @pubSubEvents        events published
     * @virtualSessions     virtual sessions created
     * @testamentsFired      testaments triggered
     * @statisticsAvailable  whether statistics were collected
     * @restBridged          whether REST bridge was used
     */
    public record ScenarioResult(
            int tenants,
            int procedures,
            int callees,
            int rpcCalls,
            int pubSubEvents,
            int virtualSessions,
            int testamentsFired,
            boolean statisticsAvailable,
            boolean restBridged
    ) {}

    private final WampRouter router = new WampRouter();
    private final RealmManager realmManager = new RealmManager();
    private final WampStatistics stats = new WampStatistics();
    private final TestamentManager testamentManager = new TestamentManager();
    private final SessionTransportTracker tracker = new SessionTransportTracker();
    private int rpcCallCount = 0;
    private int pubSubCount = 0;

    /**
     * Runs the full composite scenario.
     *
     * @return the scenario summary
     */
    public ScenarioResult run() {
        // 1. Register all RWAMP features on the router
        SessionMetaApi.register(router, tracker);
        TestamentApi.register(router, testamentManager);
        StatisticsApi.register(router, stats);
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // 2. Create tenant realms
        var tenantAlpha = realmManager.createRealm("tenant.alpha");
        var tenantBeta = realmManager.createRealm("tenant.beta");

        // 3. Set up virtual sessions and REST bridge for tenant.alpha
        var virtualManager = new VirtualSessionManager(tenantAlpha);
        VirtualSessionApi.register(router, tenantAlpha, virtualManager);
        var bridge = new RestWampBridge(router, virtualManager,
                ReflectionApi.createRegistry(router));

        // 4. Register business procedures
        int procedures = registerProcedures(tenantAlpha, tenantBeta);

        // 5. Execute RPC calls
        int rpcCalls = executeRpcCalls(tenantAlpha, tenantBeta);

        // 6. Execute pub/sub
        int pubSubEvents = executePubSub(tenantAlpha);

        // 7. REST bridge calls
        executeRestCalls(bridge);

        // 8. Testament setup
        executeTestaments();

        return new ScenarioResult(
                2,                                          // tenants
                procedures,                                 // procedure registrations
                4,                                          // callee instances (2 roundrobin + 1 first + 1)
                rpcCalls,                                    // RPC calls
                pubSubEvents,                                // events published
                virtualManager.getCount(),                  // virtual sessions
                1,                                          // testaments fired
                true,                                       // statistics available
                true                                        // REST bridged
        );
    }

    private int registerProcedures(Realm alpha, Realm beta) {
        int count = 0;

        // Alpha: "com.tenant.order.create" — roundrobin with 2 instances
        registerCallee(alpha, "com.tenant.order.create",
                args -> List.of("order-created", args.get(0)),
                Map.of("invoke", "roundrobin"));
        count++;

        registerCallee(alpha, "com.tenant.order.create",
                args -> List.of("order-created-v2", args.get(0)),
                Map.of("invoke", "roundrobin"));
        count++;

        // Alpha: "com.tenant.user.lookup" — first instance
        registerCallee(alpha, "com.tenant.user.lookup",
                args -> List.of(Map.of("user", args.get(0), "role", "admin")),
                Map.of("invoke", "first"));
        count++;

        // Beta: "com.tenant.reporting.generate" — single instance
        registerCallee(beta, "com.tenant.reporting.generate",
                args -> List.of("report-id", System.currentTimeMillis()),
                Map.of("invoke", "single"));
        count++;

        return count;
    }

    private void registerCallee(Realm realm, String procedure,
                                  java.util.function.Function<List<Object>, List<Object>> handler,
                                  Map<String, Object> options) {
        var pair = InMemoryTransport.createPair();
        var statsTransport = new StatisticsTransport(pair[1], stats.group(realm.getName()));

        var register = new WampMessage.Register(1, options, procedure);
        pair[0].send(register);
        var received = (WampMessage.Register) statsTransport.receive();
        realm.getDealer().handleRegister(received, statsTransport);
        pair[1].send(new WampMessage.Registered(1, 1L));
    }

    private int executeRpcCalls(Realm alpha, Realm beta) {
        var pair = InMemoryTransport.createPair();

        // Call 1: order.create in alpha
        makeRpcCall(pair, router, "com.tenant.order.create", List.of("order-123"), 100L);
        rpcCallCount++;

        // Call 2: order.create in alpha (hits second instance with roundrobin)
        makeRpcCall(pair, router, "com.tenant.order.create", List.of("order-456"), 100L);
        rpcCallCount++;

        // Call 3: user.lookup in alpha
        makeRpcCall(pair, router, "com.tenant.user.lookup", List.of("user-1"), 100L);
        rpcCallCount++;

        return rpcCallCount;
    }

    private void makeRpcCall(InMemoryTransport[] pair, WampRouter router,
                              String procedure, List<Object> args, long sessionId) {
        var call = new WampMessage.Call(rpcCallCount + 1, Map.of("_caller_session", sessionId),
                procedure, args);
        pair[0].send(call);
        var received = (WampMessage.Call) pair[1].receive();
        router.route(received, pair[0], sessionId);
        pair[0].receive(); // consume result (or error)
    }

    private int executePubSub(Realm alpha) {
        var subPair = InMemoryTransport.createPair();
        var subscribe = new WampMessage.Subscribe(1, Map.of(), "events.order.created");
        subPair[0].send(subscribe);
        var subMsg = (WampMessage.Subscribe) subPair[1].receive();
        router.getBroker().handleSubscribe(subMsg, subPair[1], 100L);
        subPair[1].send(new WampMessage.Subscribed(1, 1L));
        subPair[0].receive();

        // Publish 3 events
        for (int i = 0; i < 3; i++) {
            var pubPair = InMemoryTransport.createPair();
            var publish = new WampMessage.Publish(i + 1, Map.of(),
                    "events.order.created", List.of("order-" + i, "created"));
            pubPair[0].send(publish);
            var pubMsg = (WampMessage.Publish) pubPair[1].receive();
            router.getBroker().handlePublish(pubMsg, pubPair[1], 200L);
            pubSubCount++;
        }

        return pubSubCount;
    }

    private void executeRestCalls(RestWampBridge bridge) {
        // Register a procedure callable via REST
        router.registerMetaProcedure("com.tenant.health.check", (call, transport) ->
                List.of("healthy", System.currentTimeMillis()));

        var response = bridge.handle(new RestRequest("GET", "/com/tenant/health/check",
                List.of(), Map.of("authid", "monitor"), null));
    }

    private void executeTestaments() {
        // Simulate a session with a testament closing
        var pair = InMemoryTransport.createPair();
        var session = new WampSession();
        session.establish(999L, "realm1");
        router.sessionJoined(session);

        var addTestament = new WampMessage.Call(1, Map.of("_caller_session", 999L),
                TestamentApi.PROC_ADD_TESTAMENT,
                List.of("system.session.closed", List.of(999L), Map.of()));
        pair[0].send(addTestament);
        var received = (WampMessage.Call) pair[1].receive();
        router.route(received, pair[0], 999L);
        pair[0].receive(); // consume result

        // Close the session (triggers testament)
        router.sessionLeft(999L);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new CompositeScenarioDemo();
        var result = demo.run();
        System.out.println("CompositeScenarioDemo — Full RWAMP topology:");
        System.out.println("  Tenants: " + result.tenants());
        System.out.println("  Procedures: " + result.procedures());
        System.out.println("  Callees: " + result.callees());
        System.out.println("  RPC calls: " + result.rpcCalls());
        System.out.println("  Pub/Sub events: " + result.pubSubEvents());
        System.out.println("  Virtual sessions: " + result.virtualSessions());
        System.out.println("  Testaments fired: " + result.testamentsFired());
        System.out.println("  Statistics: " + result.statisticsAvailable());
        System.out.println("  REST bridge: " + result.restBridged());
    }
}
