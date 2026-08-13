package ssg.rwamp.feature.statistics;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * WAMP statistics meta procedure: {@code wamp.statistics.get}.
 * <p>
 * Registers a meta procedure on the router that returns a snapshot
 * of statistics counters. Supports an optional realm filter via
 * kwargs.
 * <p>
 * Usage:
 * <pre>{@code
 * var stats = new WampStatistics();
 * StatisticsApi.register(router, stats);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class StatisticsApi {

    /** Procedure URI: get statistics snapshot. */
    public static final String PROC_GET = "wamp.statistics.get";

    private StatisticsApi() {
        // utility class
    }

    /**
     * Registers the statistics meta procedure with the router.
     *
     * @param router the WAMP router
     * @param stats  the statistics instance to query
     */
    public static void register(WampRouter router, WampStatistics stats) {
        router.registerMetaProcedure(PROC_GET, createHandler(stats));
    }

    /**
     * Unregisters the statistics meta procedure from the router.
     *
     * @param router the WAMP router
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_GET);
    }

    private static BiFunction<WampMessage.Call, WampTransport, List<Object>> createHandler(
            WampStatistics stats) {
        return (call, transport) -> {
            String realm = null;
            if (call.options() != null && call.options().containsKey("realm")) {
                realm = (String) call.options().get("realm");
            }

            Map<String, Object> snapshot;
            if (realm != null) {
                snapshot = stats.group(realm).snapshot();
            } else {
                snapshot = stats.snapshot();
            }

            return List.of(snapshot);
        };
    }
}
