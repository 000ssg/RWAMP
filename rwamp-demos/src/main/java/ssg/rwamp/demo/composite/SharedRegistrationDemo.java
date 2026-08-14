package ssg.rwamp.demo.composite;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared registration demo — same procedure registered by multiple instances
 * with different invoke policies.
 * <p>
 * Scenario: The same procedure URI is registered by 3 different instances (simulating
 * 3 service instances in a cluster). The demo iterates through all supported invoke
 * policies to demonstrate how call distribution differs.
 * <p>
 * Invoke policies:
 * <ul>
 *   <li><b>single</b> — only one instance can register; subsequent registrations are
 *       rejected with {@code wamp.error.procedure_already_exists}</li>
 *   <li><b>first</b> — all calls go to the first registered instance</li>
 *   <li><b>last</b> — all calls go to the most recently registered instance</li>
 *   <li><b>roundrobin</b> — calls are distributed cyclically across all instances</li>
 *   <li><b>random</b> — each call is routed to a random instance</li>
 * </ul>
 * <p>
 * Resource usage comparison:
 * <ul>
 *   <li>single — lowest overhead (one instance), but no redundancy</li>
 *   <li>first/last — moderate overhead (multiple instances registered),
 *       but only one actively handles calls</li>
 *   <li>roundrobin — highest throughput, but all instances consume resources equally</li>
 *   <li>random — unpredictable distribution, may cause hotspots</li>
 * </ul>
 * <p>
 * Reliability comparison:
 * <ul>
 *   <li>single — single point of failure; if the instance dies, the procedure is unavailable</li>
 *   <li>first/last — standby instances exist but aren't automatically failover-ready
 *       (no health checking in basic WAMP)</li>
 *   <li>roundrobin — if one instance dies, subsequent calls may fail until the Dealer
 *       detects the failure</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class SharedRegistrationDemo {

    /**
     * Distribution result showing which instance handled each call.
     *
     * @param invokePolicy the policy used
     * @param instanceIds  ordered list of instance IDs that handled each call
     * @param rejectionCount number of rejected registrations (for "single" policy)
     */
    public record DistributionResult(String invokePolicy,
                                       List<Integer> instanceIds,
                                       int rejectionCount) {}

    /**
     * Full demo result showing distribution across all policies.
     *
     * @param results per-policy distribution results
     * @param instanceCount total instances registered
     * @param callCount calls made per policy
     */
    public record DemoResult(List<DistributionResult> results,
                              int instanceCount,
                              int callCount) {}

    /**
     * Runs the demo for all invoke policies.
     *
     * @param instanceCount number of service instances
     * @param callCount number of calls per policy
     * @return the demo result
     */
    public DemoResult run(int instanceCount, int callCount) {
        var results = new ArrayList<DistributionResult>();

        for (String policy : List.of("single", "first", "last", "roundrobin", "random")) {
            results.add(runForPolicy(policy, instanceCount, callCount));
        }

        return new DemoResult(List.copyOf(results), instanceCount, callCount);
    }

    /**
     * Runs the demo for a single invoke policy.
     */
    private DistributionResult runForPolicy(String policy, int instanceCount, int callCount) {
        var router = new WampRouter();
        var instancePairs = new InMemoryTransport[instanceCount][];

        int registered = 0;
        int rejected = 0;

        // Register instances
        for (int i = 0; i < instanceCount; i++) {
            instancePairs[i] = InMemoryTransport.createPair();
            var register = new WampMessage.Register(i + 1, Map.of("invoke", policy),
                    "com.example.shared");
            instancePairs[i][0].send(register);

            var received = (WampMessage.Register) instancePairs[i][1].receive();
            var response = router.getDealer().handleRegister(received, instancePairs[i][1]);
            instancePairs[i][1].send(response);

            var confirm = instancePairs[i][0].receive();
            if (confirm instanceof WampMessage.Registered) {
                registered++;
            } else if (confirm instanceof WampMessage.Error) {
                rejected++;
            }
        }

        // Make calls and track which instance handled each one
        var instanceIds = new ArrayList<Integer>();
        for (int c = 0; c < callCount; c++) {
            var callerPair = InMemoryTransport.createPair();

            var call = new WampMessage.Call(c + 1, Map.of(), "com.example.shared",
                    List.of("call-" + c));
            callerPair[0].send(call);
            var callMsg = (WampMessage.Call) callerPair[1].receive();
            router.getDealer().handleCall(callMsg, callerPair[1], (long) (1000 + c));

            // Find which instance received the Invocation
            for (int i = 0; i < instanceCount; i++) {
                var msg = instancePairs[i][0].tryReceive();
                if (msg instanceof WampMessage.Invocation invocation) {
                    instanceIds.add(i + 1);
                    var yield = new WampMessage.Yield(invocation.requestId(), Map.of(),
                            List.of("instance-" + (i + 1)));
                    instancePairs[i][0].send(yield);
                    var yielded = (WampMessage.Yield) instancePairs[i][1].receive();
                    router.getDealer().handleYield(yielded);
                    break;
                }
            }

            // Consume Result (or Error for failed calls in "single" policy)
            callerPair[0].receive();
        }

        return new DistributionResult(policy, List.copyOf(instanceIds), rejected);
    }

    /**
     * Main entry point — demonstrates all policies with 3 instances and 6 calls.
     */
    public static void main(String[] args) {
        var demo = new SharedRegistrationDemo();
        var result = demo.run(3, 6);

        System.out.println("SharedRegistrationDemo (3 instances, 6 calls per policy):");
        for (var dist : result.results()) {
            System.out.println("  " + dist.invokePolicy() + ": " + dist.instanceIds() +
                    (dist.rejectionCount() > 0 ? " (rejected: " + dist.rejectionCount() + ")" : ""));
        }
    }
}
