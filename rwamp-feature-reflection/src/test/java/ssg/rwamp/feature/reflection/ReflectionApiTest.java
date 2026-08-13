package ssg.rwamp.feature.reflection;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionApiTest {

    @Test
    void procedureList_empty() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.procedure.list", List.of()), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var args = ((WampMessage.Result) result).args();
        assertThat(args).hasSize(1);
        @SuppressWarnings("unchecked")
        var procedures = (List<String>) args.get(0);
        assertThat(procedures).hasSizeLessThanOrEqualTo(1); // may include meta procedures
    }

    @Test
    void procedureList_afterRegistration() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // Register a procedure
        var calleePair = InMemoryTransport.createPair();
        router.route(new WampMessage.Register(1L, Map.of(), "com.example.hello"),
                calleePair[1], 100L);
        calleePair[1].tryReceive(); // consume Registered

        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.procedure.list", List.of()), callerPair[1]);

        var result = callerPair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var procedures = (List<String>) ((WampMessage.Result) result).args().get(0);
        assertThat(procedures).contains("com.example.hello");
    }

    @Test
    void procedureDescribe() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // First register a procedure
        var calleePair = InMemoryTransport.createPair();
        router.route(new WampMessage.Register(1L, Map.of(), "com.example.hello"),
                calleePair[1], 100L);
        calleePair[1].tryReceive();

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.procedure.describe", List.of(List.of("com.example.hello"))),
                pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) ((WampMessage.Result) result).args().get(0);
        assertThat(details).containsKey("com.example.hello");
    }

    @Test
    void topicList_empty() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.topic.list", List.of()), pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var args = ((WampMessage.Result) result).args();
        assertThat(args).hasSize(3); // exact, prefix, wildcard lists
        @SuppressWarnings("unchecked")
        var topics = (List<String>) args.get(0);
        assertThat(topics).isEmpty();
    }

    @Test
    void topicList_afterSubscription() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // Subscribe to a topic
        var subscriberPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Subscribe(1L, Map.of(), "com.example.topic"),
                subscriberPair[1], 100L);
        subscriberPair[1].tryReceive(); // consume Subscribed

        var callerPair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.topic.list", List.of()), callerPair[1]);

        var result = callerPair[0].tryReceive();
        var args = ((WampMessage.Result) result).args();
        @SuppressWarnings("unchecked")
        var topics = (List<String>) args.get(0);
        assertThat(topics).contains("com.example.topic");
    }

    @Test
    void typeList_and_describe() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        var pair = InMemoryTransport.createPair();

        // Define a type
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflect.define",
                List.of("com.example.MyType", Map.of("kind", "type", "fields", List.of("name", "age")))),
                pair[1]);
        pair[0].tryReceive(); // consume Result

        // List types
        router.route(new WampMessage.Call(2L, Map.of(),
                "wamp.reflection.type.list", List.of()), pair[1]);
        var listResult = pair[0].tryReceive();
        @SuppressWarnings("unchecked")
        var types = (List<String>) ((WampMessage.Result) listResult).args().get(0);
        assertThat(types).contains("com.example.MyType");

        // Describe type
        router.route(new WampMessage.Call(3L, Map.of(),
                "wamp.reflection.type.describe",
                List.of(List.of("com.example.MyType"))), pair[1]);
        var descResult = pair[0].tryReceive();
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) ((WampMessage.Result) descResult).args().get(0);
        assertThat(details).containsKey("com.example.MyType");
    }

    @Test
    void unregisterRemovesProcedures() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);
        ReflectionApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "wamp.reflection.procedure.list", List.of()), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).isEqualTo("wamp.error.no_such_procedure");
    }
}
