package ssg.rwamp.feature.virtual;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * WAMP Virtual Session API — create virtual sessions for identity mapping.
 * <p>
 * Implements procedures for creating and managing virtual WAMP sessions:
 * <ul>
 *   <li>{@code virtual_session.register} — allocate a virtual session with auth context</li>
 *   <li>{@code virtual_session.unregister} — remove a virtual session</li>
 * </ul>
 * <p>
 * Virtual sessions appear in session meta procedures (list, count, get, kill)
 * and can be targeted by testament publishing.
 *
 * @since 0.1.0
 */
public final class VirtualSessionApi {

    public static final String PROC_REGISTER = "virtual_session.register";
    public static final String PROC_UNREGISTER = "virtual_session.unregister";

    private VirtualSessionApi() { /* utility class */ }

    /**
     * Registers virtual session meta procedures with the router.
     *
     * @param router      the WAMP router
     * @param realm       the WAMP realm (manages sessions)
     * @param manager     the virtual session manager
     */
    public static void register(WampRouter router, Realm realm, VirtualSessionManager manager) {
        router.registerMetaProcedure(PROC_REGISTER, createRegisterHandler(manager, router));
        router.registerMetaProcedure(PROC_UNREGISTER, createUnregisterHandler(manager));
    }

    /**
     * Unregisters virtual session meta procedures.
     */
    public static void unregister(WampRouter router) {
        router.unregisterMetaProcedure(PROC_REGISTER);
        router.unregisterMetaProcedure(PROC_UNREGISTER);
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createRegisterHandler(
            VirtualSessionManager manager, WampRouter router) {
        return (call, callerTransport) -> {
            if (call.args() == null || call.args().isEmpty()) {
                return List.of(0L);
            }

            // Extract auth info from args
            String authId = null;
            String authRole = null;
            String authMethod = null;

            if (call.args().get(0) instanceof Map<?, ?> authInfo) {
                authId = (String) authInfo.get("authid");
                authRole = (String) authInfo.get("authrole");
                authMethod = (String) authInfo.get("authmethod");
            }

            long virtualId = manager.register(authId, authRole, authMethod);
            var session = manager.getSession(virtualId);

            // Add to router's active sessions so meta procedures can see it
            if (session != null) {
                router.sessionJoined(session);
            }

            return List.of(Map.of(
                    "session", virtualId,
                    "authid", authId != null ? authId : "",
                    "authrole", authRole != null ? authRole : "",
                    "authmethod", authMethod != null ? authMethod : ""
            ));
        };
    }

    static BiFunction<WampMessage.Call, WampTransport, List<Object>> createUnregisterHandler(
            VirtualSessionManager manager) {
        return (call, callerTransport) -> {
            if (call.args() == null || call.args().isEmpty()) {
                return List.of();
            }

            long virtualId = ((Number) call.args().get(0)).longValue();
            manager.unregister(virtualId);
            return List.of();
        };
    }
}
