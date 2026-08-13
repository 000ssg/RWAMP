package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationHandlerTest {

    @Test
    void exactRegistration_delegatesToDealer() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1L, Map.of(), "com.example.hello");
        WampMessage response = handler.intercept(register, pair[1]);

        // Exact match is passed through (null = delegate to Dealer)
        assertThat(response).isNull();
        assertThat(registry.size()).isZero();
    }

    @Test
    void prefixRegistration_handledByHandler() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example.");
        WampMessage response = handler.intercept(register, pair[1]);

        assertThat(response).isInstanceOf(WampMessage.Registered.class);
        var registered = (WampMessage.Registered) response;
        assertThat(registered.requestId()).isEqualTo(1L);
        assertThat(registered.registrationId()).isEqualTo(1L);
        assertThat(registry.size()).isOne();
    }

    @Test
    void wildcardRegistration_handledByHandler() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(2L,
                Map.of("match", "wildcard"), "com.example.*");
        WampMessage response = handler.intercept(register, pair[1]);

        assertThat(response).isInstanceOf(WampMessage.Registered.class);
        var registered = (WampMessage.Registered) response;
        assertThat(registered.requestId()).isEqualTo(2L);
        assertThat(registered.registrationId()).isEqualTo(1L); // first in this registry
    }

    @Test
    void unregister_fromRegistry() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        // Register
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), pair[1]);

        // Unregister
        var response = handler.intercept(new WampMessage.Unregister(2L, 1L));
        assertThat(response).isInstanceOf(WampMessage.Unregistered.class);
        assertThat(((WampMessage.Unregistered) response).requestId()).isEqualTo(2L);
        assertThat(registry.size()).isZero();
    }

    @Test
    void unregister_notInRegistry_delegates() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var response = handler.intercept(new WampMessage.Unregister(1L, 999L));
        assertThat(response).isNull(); // delegate to Dealer
    }

    @Test
    void resolve_prefixMatch() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), pair[1]);

        var entry = handler.resolve("com.example.hello", null);
        assertThat(entry).isPresent();
        assertThat(entry.get().pattern()).isEqualTo("com.example.");
        assertThat(entry.get().transport()).isSameAs(pair[1]);
    }

    @Test
    void resolve_wildcardMatch() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "wildcard"), "com.*.bar"), pair[1]);

        var entry = handler.resolve("com.example.bar", null);
        assertThat(entry).isPresent();
    }

    @Test
    void resolve_noMatch() {
        var registry = PatternRegistry.create();
        var handler = new RegistrationHandler(registry);

        var pair = InMemoryTransport.createPair();
        handler.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), pair[1]);

        var entry = handler.resolve("com.test.hello", null);
        assertThat(entry).isEmpty();
    }
}
