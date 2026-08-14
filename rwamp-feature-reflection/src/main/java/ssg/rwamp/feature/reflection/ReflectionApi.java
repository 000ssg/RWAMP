package ssg.rwamp.feature.reflection;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.Broker;
import ssg.legoflow.wamp.core.router.Dealer;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * WAMP Reflection API — introspect procedures, topics, and definitions.
 * <p>
 * Registers meta procedures for runtime introspection of the WAMP router:
 * <ul>
 *   <li>{@code wamp.reflection.procedure.list} — list registered procedures</li>
 *   <li>{@code wamp.reflection.procedure.describe} — describe procedure details</li>
 *   <li>{@code wamp.reflection.topic.list} — list subscribed topics</li>
 *   <li>{@code wamp.reflection.topic.describe} — describe topic details</li>
 *   <li>{@code wamp.reflection.type.list} — list type/error definitions</li>
 *   <li>{@code wamp.reflection.type.describe} — describe type/error details</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * var registry = new ReflectionRegistry(router.getDealer(), router.getBroker());
 * ReflectionApi.register(router, registry);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ReflectionApi {

    public static final String PROC_PROCEDURE_LIST = "wamp.reflection.procedure.list";
    public static final String PROC_PROCEDURE_DESCRIBE = "wamp.reflection.procedure.describe";
    public static final String PROC_TOPIC_LIST = "wamp.reflection.topic.list";
    public static final String PROC_TOPIC_DESCRIBE = "wamp.reflection.topic.describe";
    public static final String PROC_TYPE_LIST = "wamp.reflection.type.list";
    public static final String PROC_TYPE_DESCRIBE = "wamp.reflection.type.describe";
    public static final String PROC_ERROR_LIST = "wamp.reflection.error.list";
    public static final String PROC_DEFINE = "wamp.reflect.define";

    private ReflectionApi() { /* utility class */ }

    /**
     * Creates a ReflectionRegistry for the given router.
     *
     * @param router the WAMP router
     * @return the registry
     */
    public static ReflectionRegistry createRegistry(WampRouter router) {
        return new ReflectionRegistry(router.getDealer(), router.getBroker());
    }

    /**
     * Registers all reflection meta procedures with the router.
     *
     * @param router   the WAMP router
     * @param registry the reflection registry
     */
    public static void register(WampRouter router, ReflectionRegistry registry) {
        router.registerMetaProcedure(PROC_PROCEDURE_LIST, procedureListHandler(registry));
        router.registerMetaProcedure(PROC_PROCEDURE_DESCRIBE, procedureDescribeHandler(registry));
        router.registerMetaProcedure(PROC_TOPIC_LIST, topicListHandler(registry));
        router.registerMetaProcedure(PROC_TOPIC_DESCRIBE, topicDescribeHandler(registry));
        router.registerMetaProcedure(PROC_TYPE_LIST, typeListHandler(registry));
        router.registerMetaProcedure(PROC_TYPE_DESCRIBE, typeDescribeHandler(registry));
        router.registerMetaProcedure(PROC_ERROR_LIST, (call, transport) ->
                List.of(List.copyOf(registry.getDefinitions().keySet())));
        router.registerMetaProcedure(PROC_DEFINE, defineHandler(registry));
    }

    /**
     * Unregisters all reflection meta procedures.
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_PROCEDURE_LIST);
        router.unregisterMetaProcedure(PROC_PROCEDURE_DESCRIBE);
        router.unregisterMetaProcedure(PROC_TOPIC_LIST);
        router.unregisterMetaProcedure(PROC_TOPIC_DESCRIBE);
        router.unregisterMetaProcedure(PROC_TYPE_LIST);
        router.unregisterMetaProcedure(PROC_TYPE_DESCRIBE);
        router.unregisterMetaProcedure(PROC_ERROR_LIST);
        router.unregisterMetaProcedure(PROC_DEFINE);
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> procedureListHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> List.of(List.copyOf(registry.getProcedures()));
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> procedureDescribeHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> {
            var details = registry.getProcedureDetails();
            if (call.args() != null && !call.args().isEmpty()) {
                // Filter by requested procedure names
                var filtered = new java.util.LinkedHashMap<String, Map<String, Object>>();
                for (String name : (List<String>) call.args().get(0)) {
                    if (details.containsKey(name)) filtered.put(name, details.get(name));
                }
                return List.of(filtered);
            }
            return List.of(details);
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> topicListHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> {
            var topics = List.copyOf(registry.getTopics());
            var prefixes = List.copyOf(registry.getPrefixTopics());
            var wildcards = List.copyOf(registry.getWildcardTopics());
            return List.of(topics, prefixes, wildcards);
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> topicDescribeHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> {
            var details = registry.getTopicDetails();
            if (call.args() != null && !call.args().isEmpty()) {
                var filtered = new java.util.LinkedHashMap<String, Map<String, Object>>();
                for (String name : (List<String>) call.args().get(0)) {
                    if (details.containsKey(name)) filtered.put(name, details.get(name));
                }
                return List.of(filtered);
            }
            return List.of(details);
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> typeListHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> List.of(List.copyOf(registry.getDefinitions().keySet()));
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> typeDescribeHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> {
            var defs = registry.getDefinitions();
            if (call.args() != null && !call.args().isEmpty()) {
                var filtered = new java.util.LinkedHashMap<String, Map<String, Object>>();
                for (String name : (List<String>) call.args().get(0)) {
                    if (defs.containsKey(name)) filtered.put(name, defs.get(name));
                }
                return List.of(filtered);
            }
            return List.of(Map.copyOf(defs));
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> defineHandler(
            ReflectionRegistry registry) {
        return (call, transport) -> {
            if (call.args() != null && call.args().size() >= 2) {
                String name = (String) call.args().get(0);
                if (call.args().get(1) instanceof Map<?, ?> meta) {
                    registry.define(name, (Map<String, Object>) meta);
                }
            }
            return List.of();
        };
    }
}
