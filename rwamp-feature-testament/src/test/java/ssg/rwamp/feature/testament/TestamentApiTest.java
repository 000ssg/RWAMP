package ssg.rwamp.feature.testament;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestamentApiTest {

    @Test
    void addTestament_storedForSession() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);

        var session = new WampSession();
        session.establish(42L, "test");
        router.sessionJoined(session);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L,
                Map.of("_caller_session", 42L, "scope", "destroyed"),
                "wamp.session.add_testament",
                List.of("com.example.testament", List.of("hello"), Map.of("key", "value"))),
                pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);

        // Verify testament is stored (internal state — simulated by publishing on close)
        // The testament will fire when session leaves
        router.sessionLeft(42L);

        // The testament publishes to the broker, which delivers to subscribers
        // (we don't have a subscriber in this test, so no message to receive)
    }

    @Test
    void testamentPublishedOnSessionLeave() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);

        var session = new WampSession();
        session.establish(42L, "test");
        router.sessionJoined(session);

        // Subscribe to testament topic before session leaves
        var subscriberPair = InMemoryTransport.createPair();
        router.getBroker().handleSubscribe(
                new WampMessage.Subscribe(1L, Map.of(), "com.example.testament"),
                subscriberPair[1], 99L);
        subscriberPair[1].tryReceive(); // consume Subscribed

        // Add testament
        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L,
                Map.of("_caller_session", 42L, "scope", "destroyed"),
                "wamp.session.add_testament",
                List.of("com.example.testament", List.of("goodbye"), Map.of())),
                callerPair[1]);
        callerPair[0].tryReceive(); // consume Result

        // Now close the session — testament should publish
        router.sessionLeft(42L);

        // Subscriber receives the event
        var event = subscriberPair[0].tryReceive();
        assertThat(event).isInstanceOf(WampMessage.Event.class);
        assertThat(((WampMessage.Event) event).args()).containsExactly("goodbye");
    }

    @Test
    void flushTestament_removesTestaments() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);

        var session = new WampSession();
        session.establish(42L, "test");
        router.sessionJoined(session);

        var pair = InMemoryTransport.createPair();

        // Add testament
        router.route(new WampMessage.Call(1L,
                Map.of("_caller_session", 42L),
                "wamp.session.add_testament",
                List.of("com.example.testament", List.of("data"), Map.of())),
                pair[1]);
        pair[0].tryReceive(); // consume Result

        // Flush testaments
        router.route(new WampMessage.Call(2L,
                Map.of("_caller_session", 42L),
                "wamp.session.flush_testament",
                List.of()),
                pair[1]);
        pair[0].tryReceive(); // consume Result

        // Subscribe to testament topic
        var subscriberPair = InMemoryTransport.createPair();
        router.getBroker().handleSubscribe(
                new WampMessage.Subscribe(1L, Map.of(), "com.example.testament"),
                subscriberPair[1], 99L);
        subscriberPair[1].tryReceive();

        // Close session — no event should fire
        router.sessionLeft(42L);
        var event = subscriberPair[0].tryReceive();
        assertThat(event).isNull();
    }

    @Test
    void testamentWithDetachedScope() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);

        var session = new WampSession();
        session.establish(42L, "test");
        router.sessionJoined(session);

        var subscriberPair = InMemoryTransport.createPair();
        router.getBroker().handleSubscribe(
                new WampMessage.Subscribe(1L, Map.of(), "com.example.detached"),
                subscriberPair[1], 99L);
        subscriberPair[1].tryReceive();

        // Add testament with detached scope
        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L,
                Map.of("_caller_session", 42L, "scope", "detached"),
                "wamp.session.add_testament",
                List.of("com.example.detached", List.of("detached-event"), Map.of())),
                callerPair[1]);
        callerPair[0].tryReceive();

        router.sessionLeft(42L);

        var event = subscriberPair[0].tryReceive();
        assertThat(event).isInstanceOf(WampMessage.Event.class);
        assertThat(((WampMessage.Event) event).args()).containsExactly("detached-event");
    }

    @Test
    void multipleTestamentsPublished() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);

        var session = new WampSession();
        session.establish(42L, "test");
        router.sessionJoined(session);

        var subscriberPair = InMemoryTransport.createPair();
        router.getBroker().handleSubscribe(
                new WampMessage.Subscribe(1L, Map.of(), "com.example.events"),
                subscriberPair[1], 99L);
        subscriberPair[1].tryReceive();

        var callerPair = InMemoryTransport.createPair();
        // Add two testaments
        router.route(new WampMessage.Call(1L,
                Map.of("_caller_session", 42L, "scope", "destroyed"),
                "wamp.session.add_testament",
                List.of("com.example.events", List.of("first"), Map.of())),
                callerPair[1]);
        callerPair[0].tryReceive();

        router.route(new WampMessage.Call(2L,
                Map.of("_caller_session", 42L, "scope", "destroyed"),
                "wamp.session.add_testament",
                List.of("com.example.events", List.of("second"), Map.of())),
                callerPair[1]);
        callerPair[0].tryReceive();

        router.sessionLeft(42L);

        var event1 = subscriberPair[0].tryReceive();
        var event2 = subscriberPair[0].tryReceive();
        assertThat(event1).isInstanceOf(WampMessage.Event.class);
        assertThat(event2).isInstanceOf(WampMessage.Event.class);
        var args1 = ((WampMessage.Event) event1).args();
        var args2 = ((WampMessage.Event) event2).args();
        assertThat(args1).contains("first");
        assertThat(args2).contains("second");
    }

    @Test
    void unregisterRemovesProcedures() {
        var router = new WampRouter();
        var manager = new TestamentManager();
        TestamentApi.register(router, manager);
        TestamentApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.add_testament",
                List.of("topic", List.of(), Map.of())), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).isEqualTo("wamp.error.no_such_procedure");
    }
}
