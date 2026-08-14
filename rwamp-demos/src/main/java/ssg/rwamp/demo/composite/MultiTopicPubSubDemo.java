package ssg.rwamp.demo.composite;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.role.Publisher;
import ssg.legoflow.wamp.core.role.Subscriber;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multi-topic Pub/Sub demo — multiple subscribers with different matching policies.
 * <p>
 * Scenario: Three subscribers listen on different topics with different matching policies:
 * exact match, prefix match, and wildcard match. Multiple publishers publish events
 * to various topics, demonstrating how events fan out to matching subscribers.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Exact topic matching — subscriber receives events only on the subscribed URI</li>
 *   <li>Prefix matching — subscriber receives events on any topic starting with the prefix</li>
 *   <li>Wildcard matching — subscriber receives events matching a wildcard pattern
 *       (e.g., "com.app.*.event" matches "com.app.user.event")</li>
 *   <li>Event fan-out — one publication delivered to all matching subscribers</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Flexible routing — subscribers choose their matching granularity</li>
 *   <li>Efficient fan-out — router delivers to all matching subscribers automatically</li>
 *   <li>Scalable — thousands of subscribers per topic</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Prefix matching is broad — may receive unwanted events</li>
 *   <li>Wildcard matching has fixed segment count — "com..bar" matches exactly
 *       "com.X.bar" but not "com.X.Y.bar"</li>
 *   <li>No message filtering — subscriber receives all matching events</li>
 *   <li>Publisher exclusion is opt-in — publisher must explicitly exclude sessions</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class MultiTopicPubSubDemo {

    /**
     * A subscriber with its matching policy and received events.
     *
     * @param name        subscriber identifier
     * @param topic       the topic URI subscribed to
     * @param matchPolicy the matching policy: "exact", "prefix", or "wildcard"
     * @param events      list of received event argument lists
     */
    public record SubscriberResult(String name, String topic, String matchPolicy,
                                     List<List<Object>> events) {}

    /**
     * Full demo result.
     *
     * @param subscribers per-subscriber results
     * @param publications list of (topic, payload) pairs published
     */
    public record DemoResult(List<SubscriberResult> subscribers,
                              List<Map.Entry<String, List<Object>>> publications) {}

    private final WampRouter router = new WampRouter();

    /**
     * Runs the demo with multiple subscribers and publishers.
     *
     * @return the demo result
     */
    public DemoResult run() {
        // Subscriber A: exact match on "events.user.login"
        var subA = createSubscriber("events.user.login", "exact", 100L);
        // Subscriber B: prefix match on "events.user."
        var subB = createSubscriber("events.user.", "prefix", 200L);
        // Subscriber C: wildcard match on "events.*.created"
        var subC = createSubscriber("events.*.created", "wildcard", 300L);

        var publications = new ArrayList<Map.Entry<String, List<Object>>>();

        // Publish events to various topics
        publish("events.user.login", List.of("user1", "login"));
        publish("events.user.logout", List.of("user2", "logout"));
        publish("events.user.created", List.of("user3", "new"));
        publish("events.order.created", List.of("order1", "placed"));
        publish("events.payment.created", List.of("payment1", "processed"));
        publish("events.system.status", List.of("ok"));

        publications.addAll(List.of(
                Map.entry("events.user.login", List.of("user1", "login")),
                Map.entry("events.user.logout", List.of("user2", "logout")),
                Map.entry("events.user.created", List.of("user3", "new")),
                Map.entry("events.order.created", List.of("order1", "placed")),
                Map.entry("events.payment.created", List.of("payment1", "processed")),
                Map.entry("events.system.status", List.of("ok"))
        ));

        // Collect received events from each subscriber
        var resultA = collectEvents(subA, "Subscriber A (exact: events.user.login)");
        var resultB = collectEvents(subB, "Subscriber B (prefix: events.user.)");
        var resultC = collectEvents(subC, "wildcard: events.*.created");

        var results = List.of(resultA, resultB, resultC);

        return new DemoResult(results, List.copyOf(publications));
    }

    /**
     * Creates a subscriber with the given topic and match policy.
     */
    private InMemoryTransport[] createSubscriber(String topic, String match, long sessionId) {
        var pair = InMemoryTransport.createPair();

        var subscribe = new WampMessage.Subscribe(1, Map.of("match", match), topic);
        pair[0].send(subscribe);

        var received = (WampMessage.Subscribe) pair[1].receive();
        var subscribed = router.getBroker().handleSubscribe(received, pair[1], sessionId);
        pair[1].send(subscribed);
        pair[0].receive(); // consume Subscribed

        return pair;
    }

    /**
     * Publishes an event to the given topic.
     */
    private void publish(String topic, List<Object> payload) {
        var pair = InMemoryTransport.createPair();
        var publish = new WampMessage.Publish(1, Map.of(), topic, payload);
        pair[0].send(publish);

        var received = (WampMessage.Publish) pair[1].receive();
        router.getBroker().handlePublish(received, pair[1], 400L);
    }

    /**
     * Collects all pending events from a subscriber's transport.
     */
    private SubscriberResult collectEvents(InMemoryTransport[] pair, String policy) {
        var events = new ArrayList<List<Object>>();
        WampMessage msg;
        while ((msg = pair[0].tryReceive()) != null) {
            if (msg instanceof WampMessage.Event event) {
                events.add(event.args() != null ? event.args() : List.of());
            }
        }

        String topic = switch (policy.split(":", 2)[0].trim()) {
            case "Subscriber A" -> "events.user.login";
            case "Subscriber B" -> "events.user.";
            default -> policy;
        };

        return new SubscriberResult(policy, topic,
                policy.contains("exact") ? "exact" :
                policy.contains("prefix") ? "prefix" : "wildcard",
                List.copyOf(events));
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new MultiTopicPubSubDemo();
        var result = demo.run();

        System.out.println("MultiTopicPubSubDemo — Event delivery matrix:");
        for (var sub : result.subscribers()) {
            System.out.println("  " + sub.name() + " [" + sub.matchPolicy() + " " +
                    sub.topic() + "]: " + sub.events().size() + " events");
            for (var evt : sub.events()) {
                System.out.println("    -> " + evt);
            }
        }
    }
}
