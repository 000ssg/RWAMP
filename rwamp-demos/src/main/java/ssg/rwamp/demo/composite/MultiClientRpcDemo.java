package ssg.rwamp.demo.composite;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.role.Caller;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Multi-client RPC demo — multiple callers invoking procedures from multiple callees.
 * <p>
 * Scenario: Three callee instances register the same procedure URI using shared
 * registration with "roundrobin" invoke policy. Multiple callers invoke the procedure
 * and receive results from different callee instances, demonstrating load distribution.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Shared procedure registration (multiple callees per procedure)</li>
 *   <li>Invoke policies: single, first, last, roundrobin, random</li>
 *   <li>Load distribution across multiple callee instances</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Horizontal scalability — add more callee instances for capacity</li>
 *   <li>Fault tolerance — if one callee fails, others can handle calls</li>
 *   <li>Load balancing — roundrobin distributes calls evenly</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Stateless procedures only — stateful procedures need sticky sessions</li>
 *   <li>Resource overhead — each callee instance consumes memory and threads</li>
 *   <li>Invoke policy negotiation — all callees must agree on the same policy;
 *       "single" policy rejects duplicate registration</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class MultiClientRpcDemo {

    /**
     * Result of a single RPC call in the demo.
     *
     * @param callerId  the caller identifier
     * @param calleeId  which callee instance handled the call (1-based)
     * @param procedure the procedure URI
     * @param result    the result arguments
     */
    public record CallResult(int callerId, int calleeId, String procedure, List<Object> result) {}

    /**
     * Full demo result with per-call breakdown.
     *
     * @param invokePolicy the invoke policy used
     * @param callees      number of callee instances registered
     * @param callers      number of caller instances
     * @param calls        list of individual call results showing callee distribution
     */
    public record DemoResult(String invokePolicy, int callees, int callers, List<CallResult> calls) {}

    private final WampRouter router = new WampRouter();

    /**
     * A callee endpoint with a transport pair and handler.
     */
    private record CalleeEndpoint(int id, InMemoryTransport[] pair,
                                   java.util.function.Function<List<Object>, List<Object>> handler) {}

    /**
     * Runs the demo with shared registration using the specified invoke policy.
     *
     * @param invokePolicy one of: "single", "first", "last", "roundrobin", "random"
     * @param calleeCount  number of callee instances to register
     * @param callerCount  number of caller instances to create
     * @return the demo result
     */
    public DemoResult run(String invokePolicy, int calleeCount, int callerCount) {
        var callees = new ArrayList<CalleeEndpoint>();

        // Register multiple callees for the same procedure with shared invoke policy
        for (int i = 0; i < calleeCount; i++) {
            var pair = InMemoryTransport.createPair();
            int calleeId = i + 1;
            var handler = (java.util.function.Function<List<Object>, List<Object>>) args ->
                    List.of("callee-" + calleeId, ((Number) args.get(0)).intValue() * 10);

            // Send Register with invoke option
            var register = new WampMessage.Register(i + 1, Map.of("invoke", invokePolicy),
                    "com.example.compute");
            pair[0].send(register);

            // Router handles the registration
            var received = (WampMessage.Register) pair[1].receive();
            var registered = router.getDealer().handleRegister(received, pair[1]);
            pair[1].send(registered);
            pair[0].receive(); // consume Registered

            callees.add(new CalleeEndpoint(calleeId, pair, handler));
        }

        // Create callers and invoke the procedure
        var results = new ArrayList<CallResult>();
        for (int i = 0; i < callerCount; i++) {
            var callerPair = InMemoryTransport.createPair();
            int callerId = i + 1;

            // Send Call message
            var call = new WampMessage.Call(i + 1, Map.of(),
                    "com.example.compute", List.of(callerId));
            callerPair[0].send(call);

            // Router routes the call — Dealer selects a callee and sends Invocation
            var receivedCall = (WampMessage.Call) callerPair[1].receive();
            router.getDealer().handleCall(receivedCall, callerPair[1], (long) (100 + i));

            // Find which callee received the Invocation
            int selectedCallee = -1;
            for (var callee : callees) {
                var msg = callee.pair()[0].tryReceive();
                if (msg instanceof WampMessage.Invocation invocation) {
                    selectedCallee = callee.id();
                    // Callee processes and yields
                    var result = callee.handler().apply(invocation.args());
                    var yield = new WampMessage.Yield(invocation.requestId(), Map.of(), result);
                    callee.pair()[0].send(yield);
                    break;
                }
            }

            // Dealer receives Yield and forwards Result to caller
            for (var callee : callees) {
                if (callee.id() == selectedCallee) {
                    var yield = (WampMessage.Yield) callee.pair()[1].receive();
                    router.getDealer().handleYield(yield);
                    break;
                }
            }

            // Caller receives Result
            var result = (WampMessage.Result) callerPair[0].receive();
            results.add(new CallResult(callerId, selectedCallee,
                    "com.example.compute", result.args()));
        }

        return new DemoResult(invokePolicy, calleeCount, callerCount, List.copyOf(results));
    }

    /**
     * Main entry point — demonstrates all invoke policies.
     */
    public static void main(String[] args) {
        var demo = new MultiClientRpcDemo();

        for (String policy : List.of("first", "last", "roundrobin", "random")) {
            var result = demo.run(policy, 3, 6);
            System.out.println("MultiClientRpcDemo (" + policy + ", 3 callees, 6 callers):");
            for (var call : result.calls()) {
                System.out.println("  Caller " + call.callerId() + " -> callee " +
                        call.calleeId() + ": " + call.result());
            }
            System.out.println();
        }

        // "single" policy rejects duplicate registration
        var singleDemo = new MultiClientRpcDemo();
        var singleResult = singleDemo.run("single", 3, 1);
        System.out.println("MultiClientRpcDemo (single, 3 callees — only first registers):");
        for (var call : singleResult.calls()) {
            if (call.calleeId() > 0)
                System.out.println("  Caller " + call.callerId() + " -> callee " +
                        call.calleeId() + ": " + call.result());
        }
    }
}
