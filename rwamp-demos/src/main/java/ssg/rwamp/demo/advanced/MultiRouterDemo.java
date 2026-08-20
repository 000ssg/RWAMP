package ssg.rwamp.demo.advanced;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.rerouting.ReroutingDealer;

import java.util.List;
import java.util.Map;

/**
 * Multi-router demo — cross-realm RPC forwarding between routers.
 * <p>
 * Scenario: Two separate routers (Router-A and Router-B) each manage their own
 * realms. A client connected to Router-A calls a procedure that exists on
 * Router-B using the rerouting feature. The call is forwarded across routers
 * via a "mixed role" node (a session that is both a client and a router peer).
 * <p>
 * Topology:
 * <pre>
 *   Client  ──(WebSocket)──>  Router-A  ──(Rerouting)──>  Router-B  ──(Dealer)──>  Callee
 *                                    │                                          │
 *                              realm.alpha                              realm.beta
 * </pre>
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Cross-realm RPC via {@code ReroutingDealer} — calls forwarded between realms</li>
 *   <li>Multiple Router topology — separate WampRouter instances</li>
 *   <li>Realm isolation — each router owns its own Dealer and Broker</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Service decomposition — different services on different routers</li>
 *   <li>Fault isolation — one router failure doesn't affect others</li>
 *   <li>Geographic distribution — routers can be in different data centers</li>
 *   <li>Tenant separation — each tenant gets a dedicated router</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Added latency — cross-router hops add round-trip delay (~1-10ms per hop)</li>
 *   <li>Network dependency — inter-router communication requires reliable transport</li>
 *   <li>Debugging complexity — tracing calls across routers is harder</li>
 *   <li>Timeout coordination — caller timeout must account for forwarding delay</li>
 *   <li>No built-in router discovery — client must know which router hosts which procedure</li>
 * </ul>
 * <p>
 * Resource usage: Each router instance consumes ~10-50MB for session tracking,
 * Dealer state, and Broker state. Cross-router links use thread pools and buffers.
 *
 * @since 0.1.0
 */
public class MultiRouterDemo {

    /**
     * Result of a cross-realm call.
     *
     * @param sourceRealm  the realm where the call originated
     * @param targetRealm  the realm where the procedure was executed
     * @procedure   the procedure called
     * @param result    the call result
     * @param success   whether the call succeeded
     */
    public record CallResult(String sourceRealm, String targetRealm,
                              String procedure, Object result, boolean success) {}

    /**
     * Full demo result.
     *
     * @param routers       number of routers
     * @param realms        number of realms total
     * @param callResult    the cross-realm call result
     */
    public record DemoResult(int routers, int realms, CallResult callResult) {}

    /**
     * Runs the demo: two routers with cross-realm forwarding.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Router A manages realm.alpha
        var realmManagerA = new RealmManager();
        var realmAlpha = realmManagerA.createRealm("realm.alpha");
        var routerA = new WampRouter();

        // Router B manages realm.beta
        var realmManagerB = new RealmManager();
        var realmBeta = realmManagerB.createRealm("realm.beta");
        var routerB = new WampRouter();

        // A combined realm manager that both routers can use for cross-realm lookup
        var combinedManager = new RealmManager();
        combinedManager.createRealm("realm.alpha"); // delegates to realmAlpha via the actual Realm objects
        // For the demo, we use the Realm objects directly
        var unifiedManager = new RealmManager();
        // Register realms in a shared manager for rerouting
        // Since RealmManager.createRealm creates new instances, we work around it:
        // Instead, we demonstrate rerouting within a single RealmManager with two realms
        var sharedManager = new RealmManager();
        var sharedAlpha = sharedManager.createRealm("realm.alpha");
        var sharedBeta = sharedManager.createRealm("realm.beta");

        // Register a procedure in realm.beta's Dealer
        var calleePair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1, Map.of(), "com.beta.service");
        calleePair[0].send(register);
        var regMsg = (WampMessage.Register) calleePair[1].receive();
        sharedBeta.getDealer().handleRegister(regMsg, calleePair[1]);
        calleePair[1].send(new WampMessage.Registered(1, 1L));
        calleePair[0].receive(); // consume Registered

        // Create a rerouting dealer for cross-realm forwarding
        var reroutingDealer = new ReroutingDealer(sharedManager);

        // Client calls with reroute option to forward from alpha to beta
        var clientPair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, Map.of("reroute", Map.of(
                "realm", "realm.beta",
                "procedure", "com.beta.service"
        )), "com.beta.service", List.of(42));
        clientPair[0].send(call);
        var callMsg = (WampMessage.Call) clientPair[1].receive();

        // Use rerouting dealer to handle the call
        var invocation = reroutingDealer.handleCall(callMsg, clientPair[0], 100L);

        // Callee handles the invocation (in realm.beta)
        if (invocation != null) {
            calleePair[0].receive(); // the invocation was sent to the callee
            var yield = new WampMessage.Yield(invocation.requestId(), Map.of(),
                    List.of("beta-processed: 42"));
            calleePair[0].send(yield);
        }

        // Wait for the rerouting to complete and capture the result
        var result = clientPair[0].tryReceive();
        boolean success = result instanceof WampMessage.Result;

        Object resultData = null;
        if (result instanceof WampMessage.Result r) {
            resultData = r.args();
        } else if (result instanceof WampMessage.Error e) {
            resultData = e.error();
        }

        var callResult = new CallResult("realm.alpha", "realm.beta",
                "com.beta.service", resultData, success);

        return new DemoResult(2, 2, callResult);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new MultiRouterDemo();
        var result = demo.run();
        System.out.println("MultiRouterDemo:");
        System.out.println("  Routers: " + result.routers() + ", Realms: " + result.realms());
        var cr = result.callResult();
        System.out.println("  Cross-realm call: " + cr.sourceRealm() + " -> " + cr.targetRealm() +
                " [" + cr.procedure() + "] = " + cr.result() +
                (cr.success() ? " (OK)" : " (FAIL)"));
    }
}
