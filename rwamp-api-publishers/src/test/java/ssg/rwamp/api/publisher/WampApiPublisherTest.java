package ssg.rwamp.api.publisher;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.wamp.WampApiPublisher;
import ssg.rwamp.feature.reflection.ReflectionApi;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WampApiPublisherTest {

    @Test
    void publish_registersProcedures() {
        var router = new WampRouter();
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("users", new ApiGroup("users", null,
                        Map.of(
                                "get-user", ApiOperation.function("get-user",
                                        List.of(ApiParameter.input("id", ApiDataType.INTEGER)),
                                        ApiDataType.STRING),
                                "list-users", ApiOperation.function("list-users", List.of(), ApiDataType.STRING)
                        ), null, null, null)
                ), null, null);

        var publisher = new WampApiPublisher(router, "com.app");
        var results = (Map<String, String>) publisher.publish(api);

        assertThat(results).containsEntry("get-user", "com.app.users.get-user");
        assertThat(results).containsEntry("list-users", "com.app.users.list-users");

        // Verify procedures are registered in the Dealer
        var procedures = router.getDealer().getRegisteredProcedures();
        assertThat(procedures).contains("com.app.users.get-user");
        assertThat(procedures).contains("com.app.users.list-users");
    }

    @Test
    void publish_createsReflectionMetadata() {
        var router = new WampRouter();
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("orders", new ApiGroup("orders", null,
                        Map.of("cancel", ApiOperation.procedure("cancel",
                                List.of(ApiParameter.input("orderId", ApiDataType.INTEGER)))),
                        null, null, null)
                ), null, null);

        new WampApiPublisher(router, "com.shop").publish(api);

        // Verify via reflection meta-procedure call (the proper way to introspect)
        var transport = new TestTransport();
        var call = new WampMessage.Call(1, Map.of(), ReflectionApi.PROC_ERROR_LIST, List.of());
        router.route(call, transport, 100L);
        
        var response = transport.lastMessage;
        assertThat(response).isInstanceOf(WampMessage.Result.class);
        var result = (WampMessage.Result) response;
        var definitions = (List<String>) result.args().get(0);
        assertThat(definitions).contains("com.shop.orders.cancel");
    }

    @Test
    void publish_customHandler() {
        var router = new WampRouter();
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("calc", new ApiGroup("calc", null,
                        Map.of("double", ApiOperation.function("double",
                                List.of(ApiParameter.input("n", ApiDataType.INTEGER)),
                                ApiDataType.INTEGER)),
                        null, null, null)
                ), null, null);

        var publisher = new WampApiPublisher(router, "math")
                .handler((op, call, transport) -> {
                    if (op.name().equals("double") && call.args() != null && !call.args().isEmpty()) {
                        return ((Number) call.args().get(0)).intValue() * 2;
                    }
                    return null;
                });
        publisher.publish(api);

        // Verify via route
        var transport = new TestTransport();
        var call = new WampMessage.Call(1, Map.of(), "math.calc.double", List.of(21));
        router.route(call, transport, 100L);
        var response = transport.lastMessage;
        assertThat(response).isInstanceOf(WampMessage.Result.class);
        var result = (WampMessage.Result) response;
        assertThat(result.args()).containsExactly(42);
    }

    @Test
    void unpublish_removesProcedures() {
        var router = new WampRouter();
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("g", new ApiGroup("g", null,
                        Map.of("op", ApiOperation.procedure("op", List.of())),
                        null, null, null)
                ), null, null);

        var publisher = new WampApiPublisher(router, "test");
        publisher.publish(api);
        assertThat(router.getDealer().getRegisteredProcedures()).contains("test.g.op");

        publisher.unpublish(api);
        assertThat(router.getDealer().getRegisteredProcedures()).doesNotContain("test.g.op");
    }

    // Minimal transport for testing
    private static class TestTransport implements ssg.legoflow.wamp.core.transport.WampTransport {
        WampMessage lastMessage;
        @Override public void send(WampMessage msg) { this.lastMessage = msg; }
        @Override public WampMessage receive() { return null; }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }
    }
}
