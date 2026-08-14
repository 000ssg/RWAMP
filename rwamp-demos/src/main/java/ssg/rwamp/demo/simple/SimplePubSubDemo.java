package ssg.rwamp.demo.simple;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.role.Publisher;
import ssg.legoflow.wamp.core.role.Subscriber;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Pub/Sub demo — basic topic subscription and event delivery through WampRouter.
 * <p>
 * Scenario: A subscriber connects to a router and subscribes to a topic.
 * A publisher publishes an event, and the subscriber receives it via the router's Broker.
 * <p>
 * Pros:
 * <ul>
 *   <li>Decouples publishers from subscribers — no direct coupling</li>
 *   <li>Supports fan-out — one event delivered to many subscribers</li>
 *   <li>Flexible matching — exact, prefix, and wildcard topic matching</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>No guaranteed delivery — events are fire-and-forget</li>
 *   <li>No ordering guarantees across multiple events</li>
 *   <li>Publisher cannot receive acknowledgment from subscribers</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class SimplePubSubDemo {

    /**
     * Result of the Pub/Sub demo.
     *
     * @param topic         the topic URI
     * @param publishedArgs the published event arguments
     * @param receivedArgs  the arguments received by the subscriber
     */
    public record DemoResult(String topic, List<Object> publishedArgs, List<Object> receivedArgs) {}

    private final WampRouter router = new WampRouter();

    /**
     * Runs the demo: subscribes to "events.test" and publishes ["hello", 42].
     *
     * @return the demo result
     */
    public DemoResult run() {
        var subPair = InMemoryTransport.createPair();
        var pubPair = InMemoryTransport.createPair();

        var subscriber = new Subscriber(subPair[0]);
        var publisher = new Publisher(pubPair[0]);

        // Collect received events
        var receivedEvents = new ArrayList<Object>();
        subscriber.onEvent(event -> receivedEvents.addAll(event.args()));

        // Subscriber subscribes to "events.test"
        subscriber.subscribe("events.test");

        // Router receives Subscribe
        var subscribeMsg = (WampMessage.Subscribe) subPair[1].receive();
        router.route(subscribeMsg, subPair[1], 100L);

        // Subscriber receives Subscribed confirmation
        subscriber.handleSubscribed((WampMessage.Subscribed) subPair[0].receive());

        // Publisher publishes an event
        List<Object> payload = List.of("hello", 42);
        publisher.publish("events.test", payload);

        // Router receives Publish and delivers to subscriber
        var publishMsg = (WampMessage.Publish) pubPair[1].receive();
        router.route(publishMsg, pubPair[1], 200L);

        // Subscriber receives the Event
        subscriber.handleEventMessage(subPair[0].receive());

        return new DemoResult("events.test", payload, List.copyOf(receivedEvents));
    }

    /**
     * Main entry point for standalone execution.
     */
    public static void main(String[] args) {
        var demo = new SimplePubSubDemo();
        var result = demo.run();
        System.out.println("SimplePubSubDemo: published [" + result.publishedArgs() +
                "] to " + result.topic() + ", received [" + result.receivedArgs() + "]");
    }
}
