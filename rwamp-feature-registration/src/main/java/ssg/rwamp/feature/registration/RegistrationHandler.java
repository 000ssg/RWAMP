package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.Map;

/**
 * Handles REGISTER and UNREGISTER messages with pattern matching support.
 * <p>
 * When a callee sends a REGISTER with a {@code match} option set to
 * "prefix" or "wildcard", the registration is stored in the
 * {@link PatternRegistry} instead of the lego-flow Dealer. This allows
 * a single registration to handle multiple procedure URIs.
 * <p>
 * Usage:
 * <pre>{@code
 * var registry = PatternRegistry.create();
 * var handler = new RegistrationHandler(registry);
 * // In your message loop, before routing to WampRouter:
 * WampMessage response = handler.intercept(msg, transport);
 * if (response != null) {
 *     transport.send(response);
 * } else {
 *     router.route(msg, transport);
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public class RegistrationHandler {

    private final PatternRegistry registry;

    /**
     * Creates a handler backed by the given registry.
     *
     * @param registry the pattern registry
     */
    public RegistrationHandler(PatternRegistry registry) {
        this.registry = registry;
    }

    /**
     * Intercepts REGISTER and UNREGISTER messages.
     * <p>
     * Returns a response message if the message was handled by this handler
     * (i.e., it was a pattern-based registration). Returns {@code null} if the
     * message should be passed through to the lego-flow Dealer (exact match).
     *
     * @param msg       the WAMP message
     * @param transport the callee's transport (may be null for UNREGISTER)
     * @return a response message, or {@code null} to pass through
     */
    public WampMessage intercept(WampMessage msg, WampTransport transport) {
        return switch (msg) {
            case WampMessage.Register register -> handleRegister(register, transport);
            case WampMessage.Unregister unregister -> handleUnregister(unregister);
            default -> null;
        };
    }

    /**
     * Handles an UNREGISTER message without needing a transport.
     * <p>
     * Looks up the registration ID in the PatternRegistry; if found, removes
     * it and returns Unregistered. Otherwise returns {@code null} (delegate to Dealer).
     *
     * @param unregister the unregister message
     * @return response or {@code null} to pass through
     */
    public WampMessage intercept(WampMessage.Unregister unregister) {
        return handleUnregister(unregister);
    }

    /**
     * Handles a REGISTER message. If the {@code match} option is "prefix" or
     * "wildcard", registers in the PatternRegistry and returns a Registered
     * message. Otherwise returns {@code null} (delegate to Dealer).
     */
    private WampMessage handleRegister(WampMessage.Register register,
                                        WampTransport transport) {
        String matchType = getMatchType(register.options());

        if (!"exact".equals(matchType)) {
            long regId = registry.register(
                    register.procedure(),
                    matchType,
                    getInvokePolicy(register.options()),
                    transport
            );
            return new WampMessage.Registered(register.requestId(), regId);
        }

        return null; // pass through to Dealer
    }

    /**
     * Handles an UNREGISTER message.
     */
    private WampMessage handleUnregister(WampMessage.Unregister unregister) {
        if (registry.unregister(unregister.registrationId())) {
            return new WampMessage.Unregistered(unregister.requestId());
        }
        return null; // pass through to Dealer
    }

    /**
     * Resolves the best matching registration for a CALL.
     * <p>
     * Checks the pattern registry first; if a pattern matches, returns the
     * entry. Falls back to empty (delegate to Dealer for exact match).
     *
     * @param procedure  the procedure URI from the Call
     * @param matchType  the match policy (from Call options), or null for all
     * @return the matching pattern entry, or empty
     */
    public java.util.Optional<PatternRegistry.PatternEntry> resolve(String procedure,
                                                                      String matchType) {
        return registry.find(procedure, matchType);
    }

    private static String getMatchType(Map<String, Object> options) {
        if (options == null) return "exact";
        var match = options.get("match");
        if (match instanceof String s && !s.isEmpty()) return s;
        return "exact";
    }

    private static String getInvokePolicy(Map<String, Object> options) {
        if (options == null) return "single";
        var invoke = options.get("invoke");
        if (invoke instanceof String s) return s;
        return "single";
    }
}
