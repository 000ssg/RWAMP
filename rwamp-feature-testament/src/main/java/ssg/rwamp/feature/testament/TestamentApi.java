package ssg.rwamp.feature.testament;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.Broker;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * WAMP Testament API — schedule events for publication on session lifecycle events.
 * <p>
 * Implements the WAMP Advanced Profile testament feature (wamp.session.add_testament,
 * wamp.session.flush_testament). Uses lego-flow's meta procedure registration and
 * session leave hooks for automatic publishing.
 * <p>
 * Usage:
 * <pre>{@code
 * var manager = new TestamentManager();
 * TestamentApi.register(router, manager);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class TestamentApi {

    /** Procedure URI: add a testament for the calling session. */
    public static final String PROC_ADD_TESTAMENT = "wamp.session.add_testament";
    /** Procedure URI: flush (remove) testaments for the calling session. */
    public static final String PROC_FLUSH_TESTAMENT = "wamp.session.flush_testament";

    private TestamentApi() { /* utility class */ }

    /**
     * Registers testament meta procedures with the router.
     * <p>
     * Registers {@link #PROC_ADD_TESTAMENT} and {@link #PROC_FLUSH_TESTAMENT}
     * as meta procedures. Also hooks the router's session leave callback
     * to auto-publish testaments when sessions close.
     *
     * @param router  the WAMP router
     * @param manager the testament manager
     */
    public static void register(WampRouter router, TestamentManager manager) {
        router.registerMetaProcedure(PROC_ADD_TESTAMENT, createAddHandler(manager));
        router.registerMetaProcedure(PROC_FLUSH_TESTAMENT, createFlushHandler(manager));

        // Hook into session lifecycle to publish testaments on close
        router.addSessionLeaveConsumer(sessionId -> {
            var broker = router.getBroker();
            manager.publishAll(sessionId, broker);
        });
    }

    /**
     * Unregisters testament meta procedures.
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_ADD_TESTAMENT);
        router.unregisterMetaProcedure(PROC_FLUSH_TESTAMENT);
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createAddHandler(
            TestamentManager manager) {
        return (call, callerTransport) -> {
            long callerSession = extractCallerSession(call);
            if (call.args() == null || call.args().isEmpty()) {
                return List.of();
            }

            String topic = (String) call.args().get(0);
            List<Object> args = call.args().size() > 1 && call.args().get(1) instanceof List<?> lst
                    ? (List<Object>) lst : List.of();
            Map<String, Object> kwargs = call.args().size() > 2 && call.args().get(2) instanceof Map<?, ?> map
                    ? (Map<String, Object>) map : Map.of();

            // Extract options
            String scope = TestamentManager.SCOPE_DESTROYED;
            Map<String, Object> pubOptions = Map.of();
            if (call.options() != null) {
                if (call.options().containsKey("scope")) {
                    scope = (String) call.options().get("scope");
                }
                if (call.options().containsKey("publish_options")) {
                    pubOptions = (Map<String, Object>) call.options().get("publish_options");
                }
            }

            manager.add(callerSession, topic, args, kwargs, scope, pubOptions);
            return List.of();
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createFlushHandler(
            TestamentManager manager) {
        return (call, callerTransport) -> {
            long callerSession = extractCallerSession(call);
            String scope = TestamentManager.SCOPE_DESTROYED;
            if (call.options() != null && call.options().containsKey("scope")) {
                scope = (String) call.options().get("scope");
            }
            manager.flush(callerSession, scope);
            return List.of();
        };
    }

    private static long extractCallerSession(WampMessage.Call call) {
        if (call.options() != null && call.options().containsKey("_caller_session")) {
            return ((Number) call.options().get("_caller_session")).longValue();
        }
        return 0;
    }
}
