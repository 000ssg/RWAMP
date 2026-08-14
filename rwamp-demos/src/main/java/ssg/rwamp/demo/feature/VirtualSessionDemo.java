package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.virtual.VirtualSessionApi;
import ssg.rwamp.feature.virtual.VirtualSessionManager;

import java.util.List;
import java.util.Map;

/**
 * Virtual session demo — creating WAMP sessions without transports.
 * <p>
 * Scenario: An HTTP-authenticated user needs to call WAMP procedures. Instead of
 * establishing a WebSocket connection, a virtual session is created with the
 * user's auth identity. The virtual session participates in pub/sub and RPC
 * like a regular session but has no transport to receive messages.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Virtual session registration — {@code virtual_session.register}</li>
 *   <li>Auth context mapping — virtual sessions carry authid/authrole/authmethod</li>
 *   <li>Session visibility — virtual sessions appear in session meta procedures</li>
 *   <li>Virtual session cleanup — {@code virtual_session.unregister}</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>REST bridge enabler — HTTP callers get WAMP identity without transport overhead</li>
 *   <li>No connection management — virtual sessions don't need keep-alive or reconnection</li>
 *   <li>Resource efficient — no transport buffers or threads for virtual sessions</li>
 *   <li>Auth integration — seamlessly maps HTTP auth to WAMP sessions</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>One-way only — virtual sessions cannot receive events or invocations</li>
 *   <li>No session lifecycle events — sessionJoined/sessionLeft must be called manually</li>
 *   <li>Session leakage — if the caller abandons the session, it persists until unregistered</li>
 *   <li>Limited use case — primarily for REST bridge and server-to-server calls</li>
 * </ul>
 * <p>
 * Memory overhead: ~200 bytes per virtual session (WampSession object).
 * Virtual sessions do not consume network buffers or thread pools.
 *
 * @since 0.1.0
 */
public class VirtualSessionDemo {

    /**
     * Result of a virtual session operation.
     *
     * @param sessionId  the virtual session ID
     * @param authId     the authentication identity
     * @param authMethod the auth method
     * @param inRouter   whether the session is visible to the router
     */
    public record SessionResult(long sessionId, String authId,
                                 String authMethod, boolean inRouter) {}

    /**
     * Full demo result.
     *
     * @param created    the virtual session that was created
     * @param totalSessions total virtual sessions in the manager
     * @param inRouter   router session count
     */
    public record DemoResult(SessionResult created, int totalSessions, int inRouter) {}

    /**
     * Runs the demo: creates a virtual session and verifies it's tracked.
     *
     * @return the demo result
     */
    public DemoResult run() {
        var router = new WampRouter();
        var realm = new Realm("realm1");
        var manager = new VirtualSessionManager(realm);

        // Register virtual session API
        VirtualSessionApi.register(router, realm, manager);

        // Create a virtual session via meta procedure
        var adminPair = InMemoryTransport.createPair();
        var registerCall = new WampMessage.Call(1, Map.of(),
                VirtualSessionApi.PROC_REGISTER,
                List.of(Map.of(
                        "authid", "http-user-42",
                        "authrole", "authenticated",
                        "authmethod", "basic"
                )));
        adminPair[0].send(registerCall);
        var received = (WampMessage.Call) adminPair[1].receive();
        router.route(received, adminPair[0], 300L);
        var result = (WampMessage.Result) adminPair[0].receive();

        var resultMap = (Map<String, Object>) result.args().get(0);
        long sessionId = ((Number) resultMap.get("session")).longValue();
        String authId = (String) resultMap.get("authid");
        String authMethod = (String) resultMap.get("authmethod");

        // Verify the session is in the manager
        boolean inManager = manager.getSession(sessionId) != null;

        // Verify the session is in the router (via sessionJoined)
        boolean inRouter = router.getActiveSessionIds().contains(sessionId);

        var sessionResult = new SessionResult(sessionId, authId, authMethod, inRouter);
        return new DemoResult(sessionResult, manager.getCount(), router.getActiveSessionCount());
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new VirtualSessionDemo();
        var result = demo.run();
        System.out.println("VirtualSessionDemo:");
        System.out.println("  Created: " + result.created());
        System.out.println("  Manager sessions: " + result.totalSessions());
        System.out.println("  Router sessions: " + result.inRouter());
    }
}
