package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationInterceptorTest {

    @Test
    void registerWithPattern() {
        var interceptor = new RegistrationInterceptor();

        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example.");
        WampMessage response = interceptor.intercept(register, pair[1]);

        assertThat(response).isInstanceOf(WampMessage.Registered.class);
        assertThat(((WampMessage.Registered) response).registrationId()).isEqualTo(1L);
    }

    @Test
    void exactRegistration_passedThrough() {
        var interceptor = new RegistrationInterceptor();

        var pair = InMemoryTransport.createPair();
        var register = new WampMessage.Register(1L, Map.of(), "com.example.hello");
        WampMessage response = interceptor.intercept(register, pair[1]);

        assertThat(response).isNull(); // delegate to Dealer
    }

    @Test
    void callDispatchedToPatternMatch() {
        var interceptor = new RegistrationInterceptor();

        // Register a pattern
        var calleePair = InMemoryTransport.createPair();
        interceptor.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.example."), calleePair[1]);

        // Send a Call — interceptor dispatches to callee
        var callerPair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(100L, Map.of(),
                "com.example.hello", List.of());
        WampMessage response = interceptor.intercept(call, callerPair[1]);

        assertThat(response).isNull(); // consumed by pattern dispatch
        var invocation = calleePair[0].tryReceive();
        assertThat(invocation).isInstanceOf(WampMessage.Invocation.class);
        assertThat(((WampMessage.Invocation) invocation).registrationId()).isEqualTo(100L);
    }

    @Test
    void yieldRoutedBackToCaller() {
        var interceptor = new RegistrationInterceptor();

        // Register
        var calleePair = InMemoryTransport.createPair();
        interceptor.intercept(new WampMessage.Register(1L,
                Map.of("match", "wildcard"), "com.*.bar"), calleePair[1]);

        // Dispatch call
        var callerPair = InMemoryTransport.createPair();
        interceptor.intercept(new WampMessage.Call(100L, Map.of(),
                "com.test.bar", List.of()), callerPair[1]);

        // Callee receives Invocation
        var invocation = calleePair[0].tryReceive();
        assertThat(invocation).isInstanceOf(WampMessage.Invocation.class);
        long invocationId = ((WampMessage.Invocation) invocation).requestId();

        // Callee yields
        boolean routed = interceptor.routeYield(new WampMessage.Yield(
                invocationId, Map.of(), List.of("hello")));
        assertThat(routed).isTrue();

        // Caller receives Result
        var result = callerPair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var args = (List<Object>) ((WampMessage.Result) result).args();
        assertThat(args).containsExactly("hello");
    }

    @Test
    void errorRoutedBackToCaller() {
        var interceptor = new RegistrationInterceptor();

        var calleePair = InMemoryTransport.createPair();
        interceptor.intercept(new WampMessage.Register(1L,
                Map.of("match", "prefix"), "com.error."), calleePair[1]);

        var callerPair = InMemoryTransport.createPair();
        interceptor.intercept(new WampMessage.Call(200L, Map.of(),
                "com.error.foo", List.of()), callerPair[1]);

        var invocation = calleePair[0].tryReceive();
        long invocationId = ((WampMessage.Invocation) invocation).requestId();

        // Callee sends error
        boolean routed = interceptor.routeCalleeError(new WampMessage.Error(
                WampMessageType.YIELD.code(), invocationId,
                Map.of(), "com.error.failed"));
        assertThat(routed).isTrue();

        var error = callerPair[0].tryReceive();
        assertThat(error).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) error).error()).isEqualTo("com.error.failed");
    }

    @Test
    void metaProceduresRegisteredAndUnregistered() {
        var interceptor = new RegistrationInterceptor();
        var router = new WampRouter();

        interceptor.registerMetaProcedures(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.registration.list", List.of()), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);

        interceptor.unregisterMetaProcedures(router);

        var pair2 = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(2L, Map.of(),
                "wamp.registration.list", List.of()), pair2[1]);

        var response = pair2[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
    }
}
