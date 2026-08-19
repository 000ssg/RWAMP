package ssg.rwamp.demo.advanced;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.role.Callee;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.rerouting.ReroutingApi;

import java.util.List;

/**
 * Demonstrates cross-realm RPC rerouting via {@link ReroutingApi}.
 * <p>
 * Two realms (alpha and beta) are managed by a single RealmManager.
 * A procedure registered on beta can be called from alpha through
 * the rerouting meta procedure {@code wamp.reroute.call}.
 * <p>
 * Topology:
 * <pre>
 *   Client ──(session)──> Realm.alpha ──(reroute)──> Realm.beta
 *                                              │
 *                                        procedure: com.example.add
 * </pre>
 *
 * @since 0.2.0
 */
public final class DistributedRoutingDemo {

    private DistributedRoutingDemo() {}

    public static void main(String[] args) {
        System.out.println("--- RWAMP Distributed Routing Demo ---");

        var realmManager = new RealmManager();

        // Create two realms
        var alpha = realmManager.createRealm("alpha");
        var beta = realmManager.createRealm("beta");
        System.out.println("  Created realms: alpha, beta");

        // Register a procedure on beta using a Callee connected to beta's dealer
        var calleePair = InMemoryTransport.createPair();
        var callee = new Callee(calleePair[0]);
        callee.register("com.example.add", calArgs -> {
            long a = ((Number) calArgs.get(0)).longValue();
            long b = ((Number) calArgs.get(1)).longValue();
            return List.of(a + b);
        });
        // Route the registration through beta's dealer
        var regMsg = (WampMessage.Register) calleePair[1].receive();
        beta.getDealer().handleRegister(regMsg, calleePair[1]);
        calleePair[1].send(new WampMessage.Registered(1, 1L));
        callee.handleRegistered((WampMessage.Registered) calleePair[0].receive());
        System.out.println("  Registered procedure 'com.example.add' on realm beta");

        // Verify registration
        boolean isRegistered = beta.getDealer().isRegistered("com.example.add");
        System.out.println("  Procedure confirmed on beta: " + isRegistered);

        // Create a router and enable rerouting between realms
        var router = new WampRouter();
        ReroutingApi.register(router, realmManager);
        System.out.println("  Registered rerouting meta procedure wamp.reroute.call");

        // Demonstrate the reroute option concept
        System.out.println("\n  Rerouting call simulation:");
        System.out.println("    OPTIONS: {reroute: {realm: 'beta', procedure: 'com.example.add'}}");

        // Show that beta has the target procedure
        var targetRealm = realmManager.getRealm("beta");
        System.out.println("  Target realm 'beta' exists: " + targetRealm.isPresent());
        System.out.println("  Target procedure 'com.example.add' registered: " +
            targetRealm.map(r -> r.getDealer().isRegistered("com.example.add")).orElse(false));

        System.out.println("\n  Demo: cross-realm rerouting configuration complete");
        System.out.println("  (Actual WAMP CALL forwarding requires transport layer + session)");

        System.out.println("\nDistributed routing demo completed.");
    }
}
