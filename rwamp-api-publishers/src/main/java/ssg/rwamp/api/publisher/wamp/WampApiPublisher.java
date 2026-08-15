package ssg.rwamp.api.publisher.wamp;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.ApiPublisher;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.reflection.ReflectionRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes an {@link ApiDefinition} as WAMP procedures on a router.
 * <p>
 * For each operation in the definition, this publisher registers a
 * corresponding WAMP procedure URI through the Dealer's registration system
 * and creates reflection metadata for runtime introspection.
 * A user-provided {@link CallHandler} resolves and executes actual calls.
 * <p>
 * Inspired by xLib's integration between {@code API_Publisher} and WAMP's
 * {@code WAMPRPCDealer}.
 * <p>
 * Usage:
 * <pre>{@code
 * var publisher = new WampApiPublisher(router, "com.app");
 * publisher.handler((op, call, transport) -> handle(op, call));
 * var result = publisher.publish(definition);
 * }</pre>
 *
 * @since 0.1.0
 */
public class WampApiPublisher implements ApiPublisher {

    private final ssg.legoflow.wamp.core.router.WampRouter router;
    private final String uriPrefix;
    private CallHandler callHandler;

    // Internal: maps procedure URI -> ApiOperation for handler dispatch
    private final Map<String, ApiOperation> uriToOp = new ConcurrentHashMap<>();
    
    // Internal: maps registrationId -> procedure URI for yield handling
    private final Map<Long, String> regIdToUri = new ConcurrentHashMap<>();
    
    // Internal: maps procedure URI -> registrationId for unpublishing
    private final Map<String, Long> uriToRegId = new ConcurrentHashMap<>();

    private long requestCounter = 1;

    /**
     * Creates a publisher that registers operations on the given router.
     *
     * @param router    the WAMP router
     * @param uriPrefix URI prefix for registered procedures (e.g. "com.app")
     */
    public WampApiPublisher(ssg.legoflow.wamp.core.router.WampRouter router, String uriPrefix) {
        this.router = Objects.requireNonNull(router);
        this.uriPrefix = Objects.requireNonNull(uriPrefix);
    }

    /**
     * Sets the handler that resolves and executes API calls.
     * <p>
     * If not set, operations return a placeholder response.
     */
    public WampApiPublisher handler(CallHandler handler) {
        this.callHandler = handler;
        return this;
    }

    @Override
    public Object publish(ApiDefinition definition) {
        var results = new LinkedHashMap<String, String>();
        var publisherTransport = new InternalTransport();

        // Create reflection registry for metadata
        var registry = ReflectionApi.createRegistry(router);

        for (var group : definition.groups().values()) {
            for (var op : group.operations().values()) {
                String uri = uriPrefix + "." + group.name() + "." + op.name();
                
                // Register through the Dealer for visibility in getRegisteredProcedures()
                WampMessage.Register register = new WampMessage.Register(
                        requestCounter++, Map.of(), uri);
                var response = router.getDealer().handleRegister(register, publisherTransport);
                
                if (response instanceof WampMessage.Registered registered) {
                    long regId = registered.registrationId();
                    uriToOp.put(uri, op);
                    regIdToUri.put(regId, uri);
                    uriToRegId.put(uri, regId);
                    results.put(op.name(), uri);

                    // Create reflection metadata
                    var meta = new LinkedHashMap<String, Object>();
                    meta.put("type", "procedure");
                    meta.put("group", group.name());
                    meta.put("description", op.description() != null ? op.description() : op.summary());
                    meta.put("parameters", op.parameters().size());
                    registry.define(uri, meta);
                }
            }
        }

        // Register reflection meta procedures if not already registered
        ReflectionApi.register(router, registry);

        return results;
    }

    @Override
    public String type() { return "wamp"; }

    /** Unregisters previously published procedures */
    public void unpublish(ApiDefinition definition) {
        for (var group : definition.groups().values()) {
            for (var op : group.operations().values()) {
                String uri = uriPrefix + "." + group.name() + "." + op.name();
                Long regId = uriToRegId.remove(uri);
                if (regId != null) {
                    regIdToUri.remove(regId);
                    router.getDealer().handleUnregister(new WampMessage.Unregister(
                            requestCounter++, regId));
                }
                uriToOp.remove(uri);
            }
        }
    }

    /**
     * Internal transport that intercepts Invocations and dispatches to the handler.
     * When the Dealer sends an Invocation, this transport handles it by calling
     * the user's CallHandler, then sends a Yield back to the Dealer.
     */
    private class InternalTransport implements WampTransport {
        
        @Override
        public void send(WampMessage msg) {
            if (msg instanceof WampMessage.Invocation invocation) {
                String uri = regIdToUri.get(invocation.registrationId());
                ApiOperation op = uri != null ? uriToOp.get(uri) : null;
                
                WampMessage.Call call = new WampMessage.Call(
                        invocation.requestId(),
                        invocation.details() != null ? invocation.details() : Map.of(),
                        uri != null ? uri : "unknown",
                        invocation.args() != null ? invocation.args() : List.of()
                );
                
                List<Object> result;
                if (callHandler != null && op != null) {
                    try {
                        Object r = callHandler.handle(op, call, null);
                        result = r instanceof List ? (List<Object>) r : List.of(r);
                    } catch (Throwable t) {
                        result = List.of(Map.of("error", t.getMessage()));
                    }
                } else {
                    result = List.of(Map.of("uri", uri != null ? uri : "unknown", "status", "registered"));
                }
                
                // Send result back via Yield
                WampMessage.Yield yield = new WampMessage.Yield(
                        invocation.requestId(), Map.of(), result);
                router.getDealer().handleYield(yield);
            }
        }

        @Override
        public WampMessage receive() { return null; }
        
        @Override
        public void close() {}
        
        @Override
        public boolean isOpen() { return true; }
    }

    /**
     * Functional interface for handling WAMP calls for a specific operation.
     */
    @FunctionalInterface
    public interface CallHandler {
        Object handle(ApiOperation operation, WampMessage.Call call,
                      WampTransport transport);
    }
}
