package ssg.rwamp.demo.simple;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.session.SessionMetaApi;
import ssg.rwamp.feature.session.SessionTransportTracker;

import java.util.List;
import java.util.Map;

/**
 * Session management demo — session tracking, kill, and interrupt via meta procedures.
 * <p>
 * Scenario: Multiple sessions join a router. An admin session kills a target session
 * using {@code wamp.session.kill}. Demonstrates RWAMP's session management features.
 * <p>
 * Pros:
 * <ul>
 *   <li>Centralized session control — admin can manage all sessions</li>
 *   <li>Session tracking — transport association enables sending GOODBYE</li>
 *   <li>Graceful shutdown — sends GOODBYE before removing the session</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Requires SessionTransportTracker — adds overhead to session tracking</li>
 *   <li>Session kill is abrupt — pending calls may be left in inconsistent state</li>
 *   <li>No built-in reconnection — killed sessions must re-establish manually</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class SessionManagementDemo {

    /**
     * Result of the session management demo.
     *
     * @param sessionsBeforeKill sessions present before kill
     * @param sessionsAfterKill  sessions remaining after kill
     * @param killedSessionId    the session that was killed
     * @param killReason         the reason sent in GOODBYE
     */
    public record DemoResult(
            List<Long> sessionsBeforeKill,
            List<Long> sessionsAfterKill,
            long killedSessionId,
            String killReason
    ) {}

    private final WampRouter router = new WampRouter();
    private final SessionTransportTracker tracker = new SessionTransportTracker();

    /**
     * Runs the demo: creates 3 sessions, kills one, and verifies the state.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Register session meta procedures
        SessionMetaApi.register(router, tracker);

        // Create 3 sessions with transports
        var pair1 = InMemoryTransport.createPair();
        var pair2 = InMemoryTransport.createPair();
        var pair3 = InMemoryTransport.createPair();

        // Simulate sessions joining the router
        var session1 = new WampSession();
        session1.establish(1001L, "realm1");
        router.sessionJoined(session1);
        tracker.track(1001L, pair1[1]);

        var session2 = new WampSession();
        session2.establish(1002L, "realm1");
        router.sessionJoined(session2);
        tracker.track(1002L, pair2[1]);

        var session3 = new WampSession();
        session3.establish(1003L, "realm1");
        router.sessionJoined(session3);
        tracker.track(1003L, pair3[1]);

        // List active sessions
        List<Long> before = List.copyOf(router.getActiveSessionIds());

        // Admin session (session1) kills session2
        long adminSession = 1001L;
        var killCall = new WampMessage.Call(1, Map.of("_caller_session", adminSession),
                SessionMetaApi.PROC_KILL, List.of(1002L, "wamp.session.killed_by_admin"));
        pair1[0].send(killCall);

        // Router routes the kill call
        var received = (WampMessage.Call) pair1[1].receive();
        router.route(received, pair1[0], adminSession);

        // Session2 receives GOODBYE
        var goodbye = (WampMessage.Goodbye) pair2[0].receive();
        pair2[0].close();

        // Get result back to admin
        var result = (WampMessage.Result) pair1[0].receive();

        // List remaining sessions
        List<Long> after = List.copyOf(router.getActiveSessionIds());

        return new DemoResult(before, after, 1002L, "wamp.session.killed_by_admin");
    }

    /**
     * Main entry point for standalone execution.
     */
    public static void main(String[] args) {
        var demo = new SessionManagementDemo();
        var result = demo.run();
        System.out.println("SessionManagementDemo: before=[" + result.sessionsBeforeKill() +
                "], after=[" + result.sessionsAfterKill() + "], killed=" + result.killedSessionId());
    }
}
