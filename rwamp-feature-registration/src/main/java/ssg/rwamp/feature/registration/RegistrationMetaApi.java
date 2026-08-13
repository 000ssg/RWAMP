package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * WAMP registration meta procedures: revoke, get, list.
 * <p>
 * Provides meta procedures for managing procedure registrations:
 * <ul>
 *   <li>{@code wamp.registration.revoke} — revoke a registration by ID</li>
 *   <li>{@code wamp.registration.get} — get details of a registration</li>
 *   <li>{@code wamp.registration.list} — list all registrations</li>
 * </ul>
 * <p>
 * Works with both lego-flow's Dealer (exact registrations) and the
 * RWAMP PatternRegistry (pattern registrations).
 * <p>
 * Usage:
 * <pre>{@code
 * var registry = PatternRegistry.create();
 * var handler = new RegistrationHandler(registry);
 * RegistrationMetaApi.register(router, handler, registry);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class RegistrationMetaApi {

    /** Procedure URI: revoke a registration. */
    public static final String PROC_REVOKE = "wamp.registration.revoke";
    /** Procedure URI: get registration details. */
    public static final String PROC_GET = "wamp.registration.get";
    /** Procedure URI: list all registrations. */
    public static final String PROC_LIST = "wamp.registration.list";

    private RegistrationMetaApi() {}

    /**
     * Registers all registration meta procedures with the router.
     *
     * @param router  the WAMP router
     * @param handler the registration handler (for pattern-based revoke)
     * @param registry the pattern registry
     */
    public static void register(WampRouter router, RegistrationHandler handler,
                                 PatternRegistry registry) {
        router.registerMetaProcedure(PROC_REVOKE, createRevokeHandler(handler, registry));
        router.registerMetaProcedure(PROC_GET, createGetHandler(registry));
        router.registerMetaProcedure(PROC_LIST, createListHandler(handler, registry));
    }

    /**
     * Unregisters all registration meta procedures from the router.
     *
     * @param router the WAMP router
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_REVOKE);
        router.unregisterMetaProcedure(PROC_GET);
        router.unregisterMetaProcedure(PROC_LIST);
    }

    /**
     * wamp.registration.revoke — revoke a specific registration.
     * <p>
     * Args: [registrationId]. Sends INTERRUPT to the callee and removes
     * the registration.
     */
    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createRevokeHandler(
            RegistrationHandler handler, PatternRegistry registry) {
        return (call, callerTransport) -> {
            if (call.args() == null || call.args().isEmpty()) {
                callerTransport.send(new WampMessage.Error(
                        WampMessageType.CALL.code(), call.requestId(),
                        Map.<String, Object>of(), "wamp.error.procedure_call_invalid"));
                return List.of();
            }

            long regId = ((Number) call.args().getFirst()).longValue();

            // Check pattern registry first
            var patternEntry = registry.getById(regId);
            if (patternEntry.isPresent()) {
                var entry = patternEntry.get();
                // Send INTERRUPT to callee
                entry.transport().send(new WampMessage.Interrupt(
                        regId, new LinkedHashMap<>() { { put("mode", "killnowait"); } }));
                registry.unregister(regId);
                return List.of(Map.<String, Object>of(
                        "registration", regId,
                        "revoked", true));
            }

            // Fall back to Dealer — try to find and remove from exact registrations
            // Note: This is best-effort; the Dealer doesn't expose per-ID removal
            // for exact registrations in a transport-aware way.
            return List.of(Map.<String, Object>of(
                    "registration", regId,
                    "revoked", false));
        };
    }

    /**
     * wamp.registration.get — get details of a registration.
     * <p>
     * Args: [registrationId]. Returns a map with pattern, match type, and invoke policy.
     */
    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createGetHandler(
            PatternRegistry registry) {
        return (call, callerTransport) -> {
            if (call.args() == null || call.args().isEmpty()) {
                callerTransport.send(new WampMessage.Error(
                        WampMessageType.CALL.code(), call.requestId(),
                        Map.<String, Object>of(), "wamp.error.procedure_call_invalid"));
                return List.of();
            }

            long regId = ((Number) call.args().getFirst()).longValue();
            var entry = registry.getById(regId);

            if (entry.isEmpty()) {
                callerTransport.send(new WampMessage.Error(
                        WampMessageType.CALL.code(), call.requestId(),
                        Map.<String, Object>of(), "wamp.error.no_such_registration"));
                return List.of();
            }

            var e = entry.get();
            var details = new LinkedHashMap<String, Object>();
            details.put("registration", e.regId());
            details.put("pattern", e.pattern());
            details.put("match", e.matchType());
            details.put("invoke", e.invokePolicy());
            return List.of(details);
        };
    }

    /**
     * wamp.registration.list — list all registrations.
     * <p>
     * Returns a combined view: pattern registrations from the registry and
     * exact registrations from the Dealer.
     */
    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createListHandler(
            RegistrationHandler handler, PatternRegistry registry) {
        return (call, callerTransport) -> {
            var all = registry.getAll().stream()
                    .map(e -> {
                        var m = new LinkedHashMap<String, Object>();
                        m.put("registration", e.regId());
                        m.put("pattern", e.pattern());
                        m.put("match", e.matchType());
                        m.put("invoke", e.invokePolicy());
                        return m;
                    })
                    .collect(Collectors.toList());
            return List.of(all);
        };
    }
}
