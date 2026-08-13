package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationMetaApiTest {

    @Test
    void revokeRegistration() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        // Register a pattern
        var calleePair = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), calleePair[1]);
        assertThat(registry.size()).isOne();

        // Revoke via meta procedure
        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.revoke", List.of(1L)), callerPair[1]);

        var result = callerPair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().getFirst();
        assertThat(details).containsEntry("revoked", true);

        // Callee received INTERRUPT
        var interrupt = calleePair[0].tryReceive();
        assertThat(interrupt).isInstanceOf(WampMessage.Interrupt.class);

        assertThat(registry.size()).isZero();
    }

    @Test
    void revokeMissingArgs() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.revoke", List.of()), pair[1]);

        var error = pair[0].tryReceive();
        assertThat(error).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) error).error())
                .isEqualTo("wamp.error.procedure_call_invalid");
    }

    @Test
    void getRegistration() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        // Register
        var calleePair = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "wildcard", "invoke", "roundrobin"), "com.*.bar"),
                calleePair[1]);

        // Get details
        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(2L, Map.of(),
                "wamp.registration.get", List.of(1L)), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().getFirst();
        assertThat(details).containsEntry("registration", 1L);
        assertThat(details).containsEntry("pattern", "com.*.bar");
        assertThat(details).containsEntry("match", "wildcard");
        assertThat(details).containsEntry("invoke", "roundrobin");
    }

    @Test
    void getUnknownRegistration() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.get", List.of(999L)), pair[1]);

        var error = pair[0].tryReceive();
        assertThat(error).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) error).error())
                .isEqualTo("wamp.error.no_such_registration");
    }

    @Test
    void listRegistrations() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        // Register two patterns
        var pair1 = InMemoryTransport.createPair();
        var pair2 = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), pair1[1]);
        handler.intercept(new WampMessage.Register(2L,
                Map.of("match", "wildcard"), "com.*.bar"), pair2[1]);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.list", List.of()), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var list = (List<Map<String, Object>>) ((WampMessage.Result) result).args().getFirst();
        assertThat(list).hasSize(2);
    }

    @Test
    void listEmpty() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);
        RegistrationMetaApi.register(router, handler, registry);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.list", List.of()), pair[1]);

        var result = pair[0].tryReceive();
        @SuppressWarnings("unchecked")
        var list = (List<Map<String, Object>>) ((WampMessage.Result) result).args().getFirst();
        assertThat(list).isEmpty();
    }

    @Test
    void unregisterRemovesMetaProcedures() {
        var router = new WampRouter();
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        RegistrationMetaApi.register(router, handler, registry);
        RegistrationMetaApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.list", List.of()), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error())
                .isEqualTo("wamp.error.no_such_procedure");
    }
}
