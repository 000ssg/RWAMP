package ssg.rwamp.demo.simple;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.role.Callee;
import ssg.legoflow.wamp.core.role.Caller;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.List;

/**
 * Simple RPC demo — basic procedure registration and call through WampRouter.
 * <p>
 * Scenario: A caller connects to a router, a callee registers a procedure,
 * and the caller invokes it. Demonstrates the simplest WAMP RPC flow.
 * <p>
 * Pros:
 * <ul>
 *   <li>Minimal overhead — direct in-memory routing</li>
 *   <li>Fast — no serialization or network latency</li>
 *   <li>Deterministic — no concurrency issues in single-threaded demo</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Single router — no distribution or fault tolerance</li>
 *   <li>No transport — doesn't demonstrate real-world connectivity</li>
 *   <li>Not suitable for cross-process or cross-machine communication</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class SimpleRpcDemo {

    /**
     * Result of the RPC demo.
     *
     * @param procedure the procedure URI called
     * @param arguments the call arguments
     * @param result    the procedure result
     * @param sessionId the caller's session ID
     */
    public record DemoResult(String procedure, List<Object> arguments, List<Object> result, long sessionId) {}

    private final WampRouter router = new WampRouter();

    /**
     * Runs the demo: registers "com.example.add" and calls it with [3, 5].
     *
     * @return the demo result
     */
    public DemoResult run() {
        var callerPair = InMemoryTransport.createPair();
        var calleePair = InMemoryTransport.createPair();

        var caller = new Caller(callerPair[0]);
        var callee = new Callee(calleePair[0]);

        // Callee registers "com.example.add"
        callee.register("com.example.add", args -> {
            int a = ((Number) args.get(0)).intValue();
            int b = ((Number) args.get(1)).intValue();
            return List.of(a + b);
        });

        // Router receives Register from callee
        var registerMsg = (WampMessage.Register) calleePair[1].receive();
        router.route(registerMsg, calleePair[1], 200L);
        // Callee receives Registered
        callee.handleRegistered((WampMessage.Registered) calleePair[0].receive());

        // Caller calls "com.example.add" with [3, 5]
        var future = caller.call("com.example.add", List.of(3, 5));

        // Router receives Call from caller
        var callMsg = (WampMessage.Call) callerPair[1].receive();
        router.route(callMsg, callerPair[1], 100L);

        // Callee receives Invocation and processes it
        var invocation = (WampMessage.Invocation) calleePair[0].receive();
        callee.handleInvocation(invocation);

        // Router receives Yield from callee
        var yieldMsg = (WampMessage.Yield) calleePair[1].receive();
        router.route(yieldMsg, calleePair[1], 200L);

        // Caller receives Result
        caller.handleResult((WampMessage.Result) callerPair[0].receive());

        return new DemoResult("com.example.add", List.of(3, 5), future.join().args(), 100L);
    }

    /**
     * Main entry point for standalone execution.
     */
    public static void main(String[] args) {
        var demo = new SimpleRpcDemo();
        var result = demo.run();
        System.out.println("SimpleRpcDemo: " + result.procedure() + "(" + result.arguments() + ") = " + result.result());
    }
}
