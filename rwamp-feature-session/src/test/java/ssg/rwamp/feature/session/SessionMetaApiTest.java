package ssg.rwamp.feature.session;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionMetaApiTest {

    @Test
    void registerAndKillSession() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var targetPair = InMemoryTransport.createPair();
        tracker.track(42L, targetPair[0]);

        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.kill",
                List.of(42L)), callerPair[1]);

        // GOODBYE to target
        var goodbye = targetPair[1].tryReceive();
        assertThat(goodbye).isInstanceOf(WampMessage.Goodbye.class);
        assertThat(((WampMessage.Goodbye) goodbye).reason()).isEqualTo("wamp.session.closed");

        // Result to caller
        var result = callerPair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().getFirst();
        assertThat(details).containsEntry("session", 42L);
        assertThat(details).containsEntry("reason", "wamp.session.closed");

        assertThat(tracker.getTransport(42L)).isEmpty();
    }

    @Test
    void killWithCustomReason() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var targetPair = InMemoryTransport.createPair();
        var callerPair = InMemoryTransport.createPair();
        tracker.track(10L, targetPair[0]);

        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.kill",
                List.of(10L, "wamp.session.shutdown")), callerPair[1]);

        var goodbye = targetPair[1].tryReceive();
        assertThat(((WampMessage.Goodbye) goodbye).reason()).isEqualTo("wamp.session.shutdown");
    }

    @Test
    void killMissingSessionId() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.kill",
                List.of()), pair[1]);

        // Handler sends Error directly, then handleMetaCall sends Result
        var error = pair[0].tryReceive();
        assertThat(error).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) error).error()).isEqualTo("wamp.error.procedure_call_invalid");

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        assertThat(((WampMessage.Result) result).args()).isEmpty();
    }

    @Test
    void killNonTrackedSessionReturnsResult() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.kill",
                List.of(999L)), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().getFirst();
        assertThat(details).containsEntry("session", 999L);
    }

    @Test
    void killAllSessions() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var t1Pair = InMemoryTransport.createPair();
        var t2Pair = InMemoryTransport.createPair();
        var callerPair = InMemoryTransport.createPair();
        tracker.track(100L, t1Pair[0]);
        tracker.track(200L, t2Pair[0]);

        router.route(new WampMessage.Call(1L, Map.of("_caller_session", 999L),
                "wamp.session.killall", List.of()), callerPair[1]);

        var gb1 = t1Pair[1].tryReceive();
        var gb2 = t2Pair[1].tryReceive();
        assertThat(gb1).isInstanceOf(WampMessage.Goodbye.class);
        assertThat(gb2).isInstanceOf(WampMessage.Goodbye.class);

        assertThat(tracker.getTrackedCount()).isZero();
    }

    @Test
    void interruptSession() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        var targetPair = InMemoryTransport.createPair();
        var callerPair = InMemoryTransport.createPair();
        tracker.track(55L, targetPair[0]);

        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.interrupt",
                List.of(55L, "skip")), callerPair[1]);

        var result = callerPair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().getFirst();
        assertThat(details).containsEntry("session", 55L);
        assertThat(details).containsEntry("mode", "skip");
    }

    @Test
    void unregisterRemovesProcedures() {
        var router = new WampRouter();
        var tracker = new SessionTransportTracker();

        SessionMetaApi.register(router, tracker);
        SessionMetaApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.session.kill",
                List.of(1L)), pair[1]);

        // After unregister, the procedure is not a meta procedure anymore.
        // Falls through to Dealer which returns no_such_procedure error.
        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).isEqualTo("wamp.error.no_such_procedure");
    }
}
