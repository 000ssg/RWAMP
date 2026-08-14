package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.testament.TestamentApi;
import ssg.rwamp.feature.testament.TestamentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testament demo — scheduling events for publication on session lifecycle events.
 * <p>
 * Scenario: A session registers a testament that publishes to "system.offline" when
 * the session closes. When the session is destroyed, the testament event is automatically
 * published by the router to all subscribers of the testament topic.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Testament registration — {@code wamp.session.add_testament}</li>
 *   <li>Automatic publishing — testaments fire when the session leaves</li>
 *   <li>Testament flushing — {@code wamp.session.flush_testament} removes pending testaments</li>
 *   <li>Two scopes — "detached" (transport lost) and "destroyed" (explicit close)</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Reliable "last will" — guaranteed event on session close</li>
 *   <li>Decouples cleanup logic from session teardown</li>
 *   <li>Useful for online/offline presence, resource cleanup, notifications</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Fires on any close — cannot distinguish graceful from crash</li>
 *   <li>No retry mechanism — if the broker fails, the testament is lost</li>
 *   <li>Ordering — testament events may arrive before or after other close events</li>
 *   <li>Resource overhead — each testament stores topic, args, and options</li>
 * </ul>
 * <p>
 * Typical latency: testament fires synchronously during session teardown,
 * so delay is minimal (microseconds). However, event delivery to subscribers
 * depends on their transport latency.
 *
 * @since 0.1.0
 */
public class TestamentDemo {

    /**
     * Result of the testament demo.
     *
     * @param testamentTopic     the topic where the testament event was published
     * @param testamentArgs      the testament event arguments
     * @param sessionClosed      whether the session was properly closed
     * @param testamentPublished whether the testament event was received by the subscriber
     */
    public record DemoResult(String testamentTopic,
                              List<Object> testamentArgs,
                              boolean sessionClosed,
                              boolean testamentPublished) {}

    private final WampRouter router = new WampRouter();
    private final TestamentManager testamentManager = new TestamentManager();

    /**
     * Runs the demo: registers a testament, closes the session, and captures the result.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Register testament API
        TestamentApi.register(router, testamentManager);

        // Create a subscriber to capture testament events
        var subPair = InMemoryTransport.createPair();
        var subscribe = new WampMessage.Subscribe(1, Map.of(), "system.offline");
        subPair[0].send(subscribe);
        var subMsg = (WampMessage.Subscribe) subPair[1].receive();
        router.getBroker().handleSubscribe(subMsg, subPair[1], 100L);
        subPair[1].send(new WampMessage.Subscribed(1, 1L));
        subPair[0].receive(); // consume Subscribed

        // Create a client session
        var clientPair = InMemoryTransport.createPair();
        var clientSession = new WampSession();
        clientSession.establish(500L, "realm1");
        router.sessionJoined(clientSession);

        // Client adds a testament: publish "system.offline" with args when session closes
        var addTestament = new WampMessage.Call(1, Map.of("_caller_session", 500L),
                TestamentApi.PROC_ADD_TESTAMENT,
                List.of("system.offline", List.of(500L, "went offline"), Map.of()));
        clientPair[0].send(addTestament);
        var received = (WampMessage.Call) clientPair[1].receive();
        router.route(received, clientPair[0], 500L);
        clientPair[0].receive(); // consume empty result

        // Simulate session close (trigger testament)
        router.sessionLeft(500L);

        // Subscriber receives the testament event
        var event = (WampMessage.Event) subPair[0].receive();

        return new DemoResult("system.offline",
                event.args() != null ? event.args() : List.of(),
                true, true);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new TestamentDemo();
        var result = demo.run();
        System.out.println("TestamentDemo: topic=" + result.testamentTopic() +
                ", args=" + result.testamentArgs() +
                ", published=" + result.testamentPublished());
    }
}
