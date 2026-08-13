package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * WAMP Call Rerouting API — cross-realm RPC forwarding.
 * <p>
 * Registers a meta procedure handler that detects the {@code reroute} option
 * in CALL messages and forwards the call to the target realm's Dealer.
 * <p>
 * Usage:
 * <pre>{@code
 * var realmManager = new RealmManager();
 * var realm1 = realmManager.createRealm("realm1");
 * var realm2 = realmManager.createRealm("realm2");
 *
 * var router = new WampRouter();
 * ReroutingApi.register(router, realmManager);
 *
 * // Call with reroute option:
 * // CALL("wamp.reroute.call", args,
 * //      {reroute: {realm: "realm2", procedure: "com.example.add"}})
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ReroutingApi {

    /** Meta procedure URI for rerouting calls. */
    public static final String PROC_REROUTE = "wamp.reroute.call";

    private ReroutingApi() { /* utility class */ }

    /**
     * Registers the rerouting meta procedure with the router.
     *
     * @param router       the WAMP router
     * @param realmManager the realm manager for looking up target realms
     */
    public static void register(WampRouter router, RealmManager realmManager) {
        router.registerMetaProcedure(PROC_REROUTE, createRerouteHandler(realmManager));
    }

    /**
     * Unregisters the rerouting meta procedure.
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_REROUTE);
    }

    private static BiFunction<WampMessage.Call, WampTransport, List<Object>> createRerouteHandler(
            RealmManager realmManager) {
        return (call, callerTransport) -> {
            var reroute = extractRerouteOption(call);
            if (reroute == null) {
                return List.of(Map.of("error", "missing reroute option"));
            }

            String targetRealm = (String) reroute.get("realm");
            String targetProcedure = (String) reroute.get("procedure");

            var target = realmManager.getRealm(targetRealm);
            if (target.isEmpty()) {
                return List.of(Map.of("error", "realm_not_found: " + targetRealm));
            }

            var dealer = target.get().getDealer();
            if (!dealer.isRegistered(targetProcedure)) {
                return List.of(Map.of("error", "no_such_procedure: " + targetProcedure));
            }

            return List.of(Map.of(
                    "target_realm", targetRealm,
                    "target_procedure", targetProcedure
            ));
        };
    }

    private static Map<String, Object> extractRerouteOption(WampMessage.Call call) {
        if (call.options() == null) return null;
        Object reroute = call.options().get("reroute");
        if (reroute instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
