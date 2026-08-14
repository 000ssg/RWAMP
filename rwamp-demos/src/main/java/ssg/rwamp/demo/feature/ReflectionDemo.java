package ssg.rwamp.demo.feature;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.demo.infrastructure.InMemoryTransport;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.reflection.ReflectionRegistry;

import java.util.List;
import java.util.Map;

/**
 * Reflection API demo — runtime introspection of procedures and topics.
 * <p>
 * Scenario: After registering several procedures and subscribing to topics,
 * a client introspects the router to discover what procedures and topics
 * are available, along with their details.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Procedure listing — {@code wamp.reflection.procedure.list}</li>
 *   <li>Procedure description — {@code wamp.reflection.procedure.describe}</li>
 *   <li>Topic listing — {@code wamp.reflection.topic.list}</li>
 *   <li>Topic description — {@code wamp.reflection.topic.describe}</li>
 *   <li>Type definitions — {@code wamp.reflection.type.list/describe}</li>
 *   <li>Dynamic type definition — {@code wamp.reflect.define}</li>
 * </ul>
 * <p>
 * Pros:
 * <ul>
 *   <li>Self-documenting API — clients discover available procedures at runtime</li>
 *   <li>Dynamic tooling — IDEs and monitoring can inspect live routers</li>
 *   <li>Service discovery — clients find procedures without hardcoding URIs</li>
 * </ul>
 * <p>
 * Cons:
 * <ul>
 *   <li>Security — exposes internal procedure names and topics</li>
 *   <li>Performance overhead — introspection queries add router load</li>
 *   <li>Metadata cost — maintaining descriptions requires registration with metadata</li>
 *   <li>Not part of WAMP spec — RWAMP-specific extension</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class ReflectionDemo {

    /**
     * Result of the reflection demo.
     *
     * @param procedures   list of discovered procedure URIs
     * @param topics       list of discovered topic URIs
     * @param typeCount    number of type definitions
     * @param procedureDetails procedure details map
     */
    public record DemoResult(List<String> procedures,
                              List<String> topics,
                              int typeCount,
                              Object procedureDetails) {}

    /**
     * Runs the demo: registers procedures, subscribes to topics, and introspects.
     *
     * @return the demo result
     */
    public DemoResult run() {
        var router = new WampRouter();
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // Register some procedures with the Dealer (not meta procedures)
        var calleePair = InMemoryTransport.createPair();
        var register1 = new WampMessage.Register(1, Map.of(), "com.app.compute.add");
        calleePair[0].send(register1);
        var regMsg1 = (WampMessage.Register) calleePair[1].receive();
        router.getDealer().handleRegister(regMsg1, calleePair[1]);
        calleePair[1].send(new WampMessage.Registered(1, 1L));

        var register2 = new WampMessage.Register(2, Map.of(), "com.app.compute.multiply");
        calleePair[0].send(register2);
        var regMsg2 = (WampMessage.Register) calleePair[1].receive();
        router.getDealer().handleRegister(regMsg2, calleePair[1]);
        calleePair[1].send(new WampMessage.Registered(2, 2L));

        // Subscribe to some topics
        var subPair = InMemoryTransport.createPair();
        var subscribe = new WampMessage.Subscribe(1, Map.of(), "events.user.login");
        subPair[0].send(subscribe);
        var subMsg = (WampMessage.Subscribe) subPair[1].receive();
        router.getBroker().handleSubscribe(subMsg, subPair[1], 100L);
        subPair[1].send(new WampMessage.Subscribed(1, 1L));

        var subscribe2 = new WampMessage.Subscribe(2, Map.of(), "events.system.status");
        subPair[0].send(subscribe2);
        var subMsg2 = (WampMessage.Subscribe) subPair[1].receive();
        router.getBroker().handleSubscribe(subMsg2, subPair[1], 200L);
        subPair[1].send(new WampMessage.Subscribed(2, 2L));

        // Register type definition via wamp.reflect.define
        var adminPair = InMemoryTransport.createPair();
        var defineCall = new WampMessage.Call(1, Map.of(),
                ReflectionApi.PROC_DEFINE,
                List.of("com.app.User", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", "string",
                                "age", "integer"
                        )
                )));
        adminPair[0].send(defineCall);
        var defReceived = (WampMessage.Call) adminPair[1].receive();
        router.route(defReceived, adminPair[0], 300L);
        adminPair[0].receive(); // consume result

        // Introspect: list procedures
        var listPair = InMemoryTransport.createPair();
        var listCall = new WampMessage.Call(2, Map.of(),
                ReflectionApi.PROC_PROCEDURE_LIST, null);
        listPair[0].send(listCall);
        var listReceived = (WampMessage.Call) listPair[1].receive();
        router.route(listReceived, listPair[0], 300L);
        var listResult = (WampMessage.Result) listPair[0].receive();
        var procedures = (List<String>) ((List) listResult.args().get(0));

        // Introspect: list topics
        var topicListCall = new WampMessage.Call(3, Map.of(),
                ReflectionApi.PROC_TOPIC_LIST, null);
        listPair[0].send(topicListCall);
        var topicListReceived = (WampMessage.Call) listPair[1].receive();
        router.route(topicListReceived, listPair[0], 300L);
        var topicListResult = (WampMessage.Result) listPair[0].receive();
        var topics = (List<String>) topicListResult.args().get(0);

        // Introspect: list types
        var typeListCall = new WampMessage.Call(4, Map.of(),
                ReflectionApi.PROC_TYPE_LIST, null);
        listPair[0].send(typeListCall);
        var typeListReceived = (WampMessage.Call) listPair[1].receive();
        router.route(typeListReceived, listPair[0], 300L);
        var typeListResult = (WampMessage.Result) listPair[0].receive();
        var types = (List<String>) typeListResult.args().get(0);

        // Get procedure details
        var describeCall = new WampMessage.Call(5, Map.of(),
                ReflectionApi.PROC_PROCEDURE_DESCRIBE, null);
        listPair[0].send(describeCall);
        var describeReceived = (WampMessage.Call) listPair[1].receive();
        router.route(describeReceived, listPair[0], 300L);
        var describeResult = (WampMessage.Result) listPair[0].receive();
        var details = describeResult.args();

        return new DemoResult(procedures, topics, types.size(), details);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        var demo = new ReflectionDemo();
        var result = demo.run();
        System.out.println("ReflectionDemo:");
        System.out.println("  Procedures: " + result.procedures());
        System.out.println("  Topics: " + result.topics());
        System.out.println("  Types: " + result.typeCount());
    }
}
