package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;

/**
 * Interceptor that wires pattern-based registration into a WAMP router.
 * <p>
 * This class acts as a message filter placed between the transport and the
 * router. It intercepts REGISTER, UNREGISTER, and CALL messages:
 * <ul>
 *   <li>REGISTER with a pattern match type is handled by the PatternRegistry</li>
 *   <li>UNREGISTER for a pattern registration removes it from the PatternRegistry</li>
 *   <li>CALL matching a pattern is dispatched directly to the matching callee</li>
 * </ul>
 * <p>
 * Messages not handled by this interceptor are passed through to the router
 * (exact registrations and all other message types).
 * <p>
 * Usage:
 * <pre>{@code
 * var interceptor = new RegistrationInterceptor();
 * // In your message handler:
 * WampMessage response = interceptor.intercept(msg, transport);
 * if (response != null) {
 *     transport.send(response);
 * } else {
 *     router.route(msg, transport);
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public class RegistrationInterceptor {

    private final PatternRegistry registry;
    private final RegistrationHandler handler;

    /**
     * Creates an interceptor with a new PatternRegistry and handler.
     */
    public RegistrationInterceptor() {
        this.registry = PatternRegistry.create();
        this.handler = new RegistrationHandler(registry);
    }

    /**
     * Creates an interceptor with the given registry.
     *
     * @param registry the pattern registry to use
     */
    public RegistrationInterceptor(PatternRegistry registry) {
        this.registry = registry;
        this.handler = new RegistrationHandler(registry);
    }

    /**
     * Returns the pattern registry.
     */
    public PatternRegistry registry() {
        return registry;
    }

    /**
     * Returns the registration handler.
     */
    public RegistrationHandler handler() {
        return handler;
    }

    /**
     * Intercepts a WAMP message.
     * <p>
     * Handles REGISTER/UNREGISTER for pattern-based registrations and CALL
     * dispatch for pattern-matched procedures. Returns a response message if
     * handled, or {@code null} to pass through to the router.
     *
     * @param msg       the WAMP message
     * @param transport the client's transport
     * @return a response message, or {@code null} to pass through
     */
    public WampMessage intercept(WampMessage msg, WampTransport transport) {
        // Handle REGISTER/UNREGISTER
        var regResponse = handler.intercept(msg, transport);
        if (regResponse != null) return regResponse;

        // Handle CALL for pattern-matched procedures
        if (msg instanceof WampMessage.Call call) {
            return handleCall(call, transport);
        }

        return null;
    }

    /**
     * Dispatches a CALL to a pattern-matched callee.
     * <p>
     * Checks the Call's {@code match} option (or uses default priority order).
     * If a pattern registration matches, forwards the Invocation directly to
     * the callee's transport.
     *
     * @param call      the Call message
     * @param transport the caller's transport
     * @return {@code null} if dispatched (or no match — pass to Dealer)
     */
    private WampMessage handleCall(WampMessage.Call call, WampTransport transport) {
        String matchType = getMatchType(call.options());
        var entry = handler.resolve(call.procedure(), matchType);

        if (entry.isEmpty()) return null; // no pattern match — pass to Dealer

        var reg = entry.get();
        long invocationId = System.nanoTime();
        var invocation = new WampMessage.Invocation(
                invocationId,
                call.requestId(),
                call.options() != null ? call.options() : Map.of(),
                call.args() != null ? call.args() : List.of()
        );
        reg.transport().send(invocation);
        trackInvocation(invocationId, transport, call.requestId());

        return null; // consumed by pattern dispatch
    }

    /**
     * Registers meta procedures with the router.
     *
     * @param router the WAMP router
     */
    public void registerMetaProcedures(WampRouter router) {
        RegistrationMetaApi.register(router, handler, registry);
    }

    /**
     * Unregisters meta procedures from the router.
     *
     * @param router the WAMP router
     */
    public void unregisterMetaProcedures(WampRouter router) {
        RegistrationMetaApi.unregister(router);
    }

    // ── Invocation tracking ──

    private final java.util.Map<Long, InvocationInfo> invocations =
            new java.util.concurrent.ConcurrentHashMap<>();

    record InvocationInfo(WampTransport callerTransport, long callRequestId) {}

    private void trackInvocation(long invocationId, WampTransport callerTransport,
                                  long callRequestId) {
        invocations.put(invocationId, new InvocationInfo(callerTransport, callRequestId));
    }

    /**
     * Routes a Yield from a callee back to the caller.
     * <p>
     * Call this when a Yield message is received from a transport that may be
     * handling pattern-based invocations. The Yield's {@code requestId} field
     * is the invocation ID.
     *
     * @param yield the Yield message
     * @return {@code true} if the invocation was found and the Result was sent
     */
    public boolean routeYield(WampMessage.Yield yield) {
        var info = invocations.remove(yield.requestId());
        if (info == null) return false;

        info.callerTransport().send(new WampMessage.Result(
                info.callRequestId(),
                yield.options() != null ? yield.options() : Map.of(),
                yield.args() != null ? yield.args() : List.of()
        ));
        return true;
    }

    /**
     * Routes an Error from a callee back to the caller.
     * <p>
     * The Error's {@code requestId} is treated as the invocation ID.
     *
     * @param error the Error message
     * @return {@code true} if the invocation was found and the Error was forwarded
     */
    public boolean routeCalleeError(WampMessage.Error error) {
        var info = invocations.remove(error.requestId());
        if (info == null) return false;

        info.callerTransport().send(new WampMessage.Error(
                WampMessageType.CALL.code(), info.callRequestId(),
                error.details(), error.error()
        ));
        return true;
    }

    private static String getMatchType(Map<String, Object> options) {
        if (options == null) return null; // null = search all types
        var match = options.get("match");
        if (match instanceof String s && !s.isEmpty()) return s;
        return null;
    }
}
