package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.session.SessionMetaApi;
import ssg.rwamp.feature.session.SessionTransportTracker;
import ssg.rwamp.feature.statistics.StatisticsApi;
import ssg.rwamp.feature.statistics.StatisticsTransport;
import ssg.rwamp.feature.statistics.WampStatistics;

import java.util.List;
import java.util.Map;

/**
 * Statistics demo — tracking WAMP message flow and operation counts.
 * <p>
 * Scenario: A router is instrumented with statistics tracking. Multiple RPC calls
 * and pub/sub operations are performed, then statistics are queried via the
 * {@code wamp.statistics.get} meta procedure.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Statistics tracking — wrapping transports with StatisticsTransport</li>
 *   <li>Per-realm counters — separate counters for each realm</li>
 *   <li>Meta procedure query — {@code wamp.statistics.get} returns a snapshot</li>
 *   <li>Counter categories: calls, results, errors, publishes, events, subscribes, registers</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Operational visibility — see call volume, error rates, event throughput</li>
 *   <li>Per-realm granularity — monitor multi-tenant systems separately</li>
 *   <li>Low overhead — atomic counters add minimal latency</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Memory usage — one CounterGroup per realm</li>
 *   <li>No time-series — counters are cumulative, not histogram-based</li>
 *   <li>Manual wiring — each transport must be wrapped with StatisticsTransport</li>
 *   <li>Counters are monotonic — reset() clears but no historical trend</li>
 * </ul>
 * <p>
 * Typical overhead: ~50ns per message (atomic increment). Negligible for
 * most workloads but measurable at very high throughput (100K+ msg/s).
 *
 * @since 0.1.0
 */
public class StatisticsDemo {

    /**
     * Result of the statistics demo.
     *
     * @param snapshot the statistics snapshot map
     * @param totalMessages total messages tracked (in + out)
     */
    public record DemoResult(Map<String, Object> snapshot, long totalMessages) {}

    /**
     * Runs the demo: performs several operations and queries statistics.
     *
     * @return the demo result
     */
    public DemoResult run() {
        var router = new WampRouter();
        var stats = new WampStatistics();
        var tracker = new SessionTransportTracker();

        // Register APIs
        StatisticsApi.register(router, stats);
        SessionMetaApi.register(router, tracker);

        // Create statistics-wrapped transport for an admin session
        var adminPair = InMemoryTransport.createPair();
        var realm = "realm1";
        var statsTransport = new StatisticsTransport(adminPair[1], stats.group(realm));
        tracker.track(300L, statsTransport);

        // Perform some operations to generate statistics
        var calleePair = InMemoryTransport.createPair();
        var calleeStatsTransport = new StatisticsTransport(calleePair[1], stats.group(realm));

        // Register a procedure (increments registers counter)
        var register = new WampMessage.Register(1, Map.of(), "com.example.add");
        calleePair[0].send(register);
        var received = (WampMessage.Register) calleeStatsTransport.receive();
        router.getDealer().handleRegister(received, calleeStatsTransport);
        calleePair[1].send(new WampMessage.Registered(1, 1L));
        calleePair[0].receive(); // consume Registered

        // Make a call (increments calls, messagesIn, messagesOut)
        var callerPair = InMemoryTransport.createPair();
        var callerStatsTransport = new StatisticsTransport(callerPair[1], stats.group(realm));
        var call = new WampMessage.Call(1, Map.of(), "com.example.add", List.of(3, 5));
        callerPair[0].send(call);
        var callMsg = (WampMessage.Call) callerStatsTransport.receive();
        router.getDealer().handleCall(callMsg, callerStatsTransport, 100L);

        // Callee handles invocation and yields
        var invocation = (WampMessage.Invocation) calleePair[0].receive();
        var yield = new WampMessage.Yield(invocation.requestId(), Map.of(), List.of(8));
        calleePair[0].send(yield);
        var yielded = (WampMessage.Yield) calleeStatsTransport.receive();
        router.getDealer().handleYield(yielded);

        // Caller receives result
        callerPair[0].receive();

        // Query statistics via meta procedure
        var getStatsCall = new WampMessage.Call(2, Map.of("realm", realm),
                StatisticsApi.PROC_GET, null);
        adminPair[0].send(getStatsCall);
        var statsCall = (WampMessage.Call) adminPair[1].receive();
        router.route(statsCall, statsTransport, 300L);
        var statsResult = (WampMessage.Result) adminPair[0].receive();

        var snapshot = (Map<String, Object>) statsResult.args().get(0);
        var realmStats = (Map<String, Object>) snapshot.get(realm);
        long msgsIn = ((Number) realmStats.get("messages_in")).longValue();
        long msgsOut = ((Number) realmStats.get("messages_out")).longValue();

        return new DemoResult(snapshot, msgsIn + msgsOut);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new StatisticsDemo();
        var result = demo.run();
        System.out.println("StatisticsDemo:");
        System.out.println("  Snapshot: " + result.snapshot());
        System.out.println("  Total messages: " + result.totalMessages());
    }
}
