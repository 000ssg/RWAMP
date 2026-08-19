package ssg.rwamp.demo.composite;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.role.Callee;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-realm demo — procedure and topic isolation across WAMP realms.
 * <p>
 * Scenario: Two separate realms ("realm.alpha" and "realm.beta") each get their own
 * procedures and topics. Procedures registered in one realm are invisible to the other,
 * and events published in one realm never leak to subscribers in another realm.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Realm-level isolation — each realm has its own Broker and Dealer</li>
 *   <li>Procedure namespace isolation — same procedure URI can be registered in each realm</li>
 *   <li>Topic namespace isolation — events are scoped to the realm where they're published</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Tenant isolation — separate realms for different organizations or services</li>
 *   <li>Security boundary — procedures and topics are isolated per realm</li>
 *   <li>Resource partitioning — each realm manages its own session space</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>No cross-realm communication — requires explicit bridging (see ReroutingDemo)</li>
 *   <li>Duplicated registrations — same procedure must be registered in each realm</li>
 *   <li>Operational complexity — managing multiple realms increases deployment overhead</li>
 *   <li>Each realm adds memory for Broker/Dealer state</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class MultiRealmDemo {

    /**
     * RPC result from a single realm.
     *
     * @param realmName the realm name
     * @param procedure the procedure called
     * @param result    the call result
     */
    public record RpcResult(String realmName, String procedure, List<Object> result) {}

    /**
     * Pub/Sub result from a single realm.
     *
     * @param realmName the realm name
     * @param topic     the topic published to
     * @param received  the events received
     */
    public record PubSubResult(String realmName, String topic, List<Object> received) {}

    /**
     * Full demo result showing cross-realm behavior.
     *
     * @param rpcResults      per-realm RPC results
     * @param pubSubResults   per-realm pub/sub results
     * @param realmCount      total number of realms
     * @param isolationVerified true if realm isolation is confirmed
     */
    public record DemoResult(List<RpcResult> rpcResults,
                              List<PubSubResult> pubSubResults,
                              int realmCount,
                              boolean isolationVerified) {}

    private final RealmManager realmManager = new RealmManager();

    /**
     * Runs the demo: registers procedures and topics in two realms,
     * verifies isolation, and collects results.
     *
     * @return the demo result
     */
    public DemoResult run() {
        var alpha = realmManager.createRealm("realm.alpha");
        var beta = realmManager.createRealm("realm.beta");

        // RPC in alpha realm
        var alphaRpc = runRpcInRealm(alpha, "com.alpha.double", args -> {
            int val = ((Number) args.get(0)).intValue();
            return List.of(val * 2);
        }, List.of(10));

        // RPC in beta realm
        var betaRpc = runRpcInRealm(beta, "com.beta.negate", args -> {
            int val = ((Number) args.get(0)).intValue();
            return List.of(-val);
        }, List.of(7));

        // Pub/Sub in alpha realm
        var alphaPubSub = runPubSubInRealm(alpha, "alpha.events", List.of("alpha-msg"));

        // Pub/Sub in beta realm
        var betaPubSub = runPubSubInRealm(beta, "beta.events", List.of("beta-msg"));

        // Verify isolation: procedure registered in alpha is not visible in beta
        boolean isolated = verifyIsolation(alpha, beta);

        var rpcResults = List.of(alphaRpc, betaRpc);
        var pubSubResults = List.of(alphaPubSub, betaPubSub);

        return new DemoResult(rpcResults, pubSubResults, realmManager.getRealmCount(), isolated);
    }

    private RpcResult runRpcInRealm(Realm realm, String procedure,
                                     java.util.function.Function<List<Object>, List<Object>> handler,
                                     List<Object> args) {
        var callerPair = InMemoryTransport.createPair();
        var calleePair = InMemoryTransport.createPair();
        var dealer = realm.getDealer();

        // Register callee
        var register = new WampMessage.Register(1, Map.of(), procedure);
        calleePair[0].send(register);
        var received = (WampMessage.Register) calleePair[1].receive();
        var registered = dealer.handleRegister(received, calleePair[1]);
        calleePair[1].send(registered);
        calleePair[0].receive(); // consume Registered

        // Call procedure
        var call = new WampMessage.Call(1, Map.of(), procedure, args);
        callerPair[0].send(call);
        var callMsg = (WampMessage.Call) callerPair[1].receive();
        dealer.handleCall(callMsg, callerPair[1], 100L);

        // Callee handles invocation and yields
        var invocation = (WampMessage.Invocation) calleePair[0].receive();
        var result = handler.apply(invocation.args());
        var yield = new WampMessage.Yield(invocation.requestId(), Map.of(), result);
        calleePair[0].send(yield);
        var yielded = (WampMessage.Yield) calleePair[1].receive();
        dealer.handleYield(yielded);

        var resultMsg = (WampMessage.Result) callerPair[0].receive();
        return new RpcResult(realm.getName(), procedure, resultMsg.args());
    }

    private PubSubResult runPubSubInRealm(Realm realm, String topic, List<Object> payload) {
        var subPair = InMemoryTransport.createPair();
        var pubPair = InMemoryTransport.createPair();
        var broker = realm.getBroker();

        // Subscribe
        var subscribe = new WampMessage.Subscribe(1, Map.of(), topic);
        subPair[0].send(subscribe);
        var subMsg = (WampMessage.Subscribe) subPair[1].receive();
        var subscribed = broker.handleSubscribe(subMsg, subPair[1], 100L);
        subPair[1].send(subscribed);
        subPair[0].receive(); // consume Subscribed

        // Publish
        var publish = new WampMessage.Publish(1, Map.of(), topic, payload);
        pubPair[0].send(publish);
        var pubMsg = (WampMessage.Publish) pubPair[1].receive();
        broker.handlePublish(pubMsg, pubPair[1], 200L);

        // Collect event
        var event = (WampMessage.Event) subPair[0].receive();
        return new PubSubResult(realm.getName(), topic, event.args());
    }

    private boolean verifyIsolation(Realm alpha, Realm beta) {
        // Register a procedure only in alpha
        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1, Map.of(), "com.iso.test");
        pair[0].send(register);
        var received = (WampMessage.Register) pair[1].receive();
        alpha.getDealer().handleRegister(received, pair[1]);

        return alpha.getDealer().isRegistered("com.iso.test")
                && !beta.getDealer().isRegistered("com.iso.test");
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new MultiRealmDemo();
        var result = demo.run();

        System.out.println("MultiRealmDemo:");
        for (var rpc : result.rpcResults()) {
            System.out.println("  RPC [" + rpc.realmName() + "]: " + rpc.procedure() +
                    " -> " + rpc.result());
        }
        for (var pubsub : result.pubSubResults()) {
            System.out.println("  PubSub [" + pubsub.realmName() + "]: " + pubsub.topic() +
                    " -> " + pubsub.received());
        }
        System.out.println("  Realms: " + result.realmCount() +
                ", Isolation verified: " + result.isolationVerified());
    }
}
