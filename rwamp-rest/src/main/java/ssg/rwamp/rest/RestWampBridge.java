package ssg.rwamp.rest;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;
import ssg.rwamp.feature.reflection.ReflectionRegistry;
import ssg.rwamp.feature.virtual.VirtualSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges HTTP requests to WAMP calls.
 * <p>
 * Maps HTTP method + path to a WAMP procedure URI, converts parameters
 * to WAMP arguments, executes through the router, and returns an HTTP response.
 * <p>
 * Meta procedures (registered on the router) and regular procedures
 * (registered on the Dealer) are both supported.
 * <p>
 * Usage:
 * <pre>{@code
 * var manager = new VirtualSessionManager(realm);
 * var registry = ReflectionApi.createRegistry(router);
 * var bridge = new RestWampBridge(router, manager, registry);
 * var response = bridge.handle(request);
 * }</pre>
 *
 * @since 0.1.0
 */
public class RestWampBridge {

    private final WampRouter router;
    private final VirtualSessionManager virtualSessionManager;
    private final ReflectionRegistry reflectionRegistry;

    /** Maps auth identity to virtual session ID for session reuse */
    private final Map<String, Long> authIdToSession = new ConcurrentHashMap<>();

    /**
     * Creates a new REST bridge.
     *
     * @param router              the WAMP router to forward calls to
     * @param virtualSessionManager manages virtual sessions for HTTP users
     * @param reflectionRegistry  used to discover and validate procedures
     */
    public RestWampBridge(WampRouter router,
                          VirtualSessionManager virtualSessionManager,
                          ReflectionRegistry reflectionRegistry) {
        this.router = router;
        this.virtualSessionManager = virtualSessionManager;
        this.reflectionRegistry = reflectionRegistry;
    }

    /**
     * Handles a REST request and returns a bridged WAMP response.
     *
     * @param request the incoming HTTP request
     * @return the HTTP response with WAMP result or error
     */
    public RestResponse handle(RestRequest request) {
        String procedure = resolveProcedure(request.path());
        if (procedure == null) {
            return RestResponse.notFound("no procedure mapped for path: " + request.path());
        }

        // Create or reuse virtual session for the caller
        long sessionId = getOrCreateVirtualSession(request);

        // Build WAMP call
        var args = buildArgs(request);
        var options = Map.<String, Object>of("_caller_session", sessionId);
        var call = new WampMessage.Call(0, options, procedure, args);

        // Execute: use a collecting transport to capture the Result
        var resultTransport = new ResultTransport();
        router.route(call, resultTransport, sessionId);

        WampMessage response = resultTransport.captured();
        if (response instanceof WampMessage.Result result) {
            var resultArgs = result.args();
            if (resultArgs == null || resultArgs.isEmpty()) {
                return RestResponse.notFound("no_such_procedure: " + procedure);
            }
            return RestResponse.ok(resultArgs);
        } else if (response instanceof WampMessage.Error err) {
            return RestResponse.serverError(err.error());
        }
        return RestResponse.notFound("no_such_procedure: " + procedure);
    }

    /**
     * Handles a REST publish request (POST to topic URI).
     *
     * @param request the incoming HTTP request
     * @return the HTTP response with publication ID
     */
    public RestResponse publish(RestRequest request) {
        String topic = resolveProcedure(request.path());
        if (topic == null) {
            return RestResponse.notFound("no topic mapped for path: " + request.path());
        }

        var args = buildArgs(request);
        var options = Map.<String, Object>of("exclude_me", false);
        long sessionId = getOrCreateVirtualSession(request);

        var publish = new WampMessage.Publish(0, options, topic, args);
        var published = router.getBroker().handlePublish(publish, null, sessionId);

        return RestResponse.ok(Map.of(
                "publication_id", published != null ? published.publicationId() : 0,
                "topic", topic
        ));
    }

    // ── Session management ──

    /**
     * Returns the set of tracked virtual session IDs.
     */
    public Map<String, Long> getTrackedSessions() {
        return Map.copyOf(authIdToSession);
    }

    /**
     * Removes a tracked virtual session by auth ID.
     */
    public void removeSession(String authId) {
        Long sessionId = authIdToSession.remove(authId);
        if (sessionId != null) {
            virtualSessionManager.unregister(sessionId);
            router.sessionLeft(sessionId);
        }
    }

    // ── Internal helpers ──

    private String resolveProcedure(String path) {
        String cleaned = path.strip();
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty()) return null;
        return cleaned.replace("/", ".");
    }

    private long getOrCreateVirtualSession(RestRequest request) {
        String authId = "anonymous";
        if (request.queryParams() != null && request.queryParams().containsKey("authid")) {
            authId = request.queryParams().get("authid");
        }

        return authIdToSession.computeIfAbsent(authId, id -> {
            long sessionId = virtualSessionManager.register(id, "anonymous", "rest");
            WampSession session = virtualSessionManager.getSession(sessionId);
            if (session != null) {
                router.sessionJoined(session);
            }
            return sessionId;
        });
    }

    private List<Object> buildArgs(RestRequest request) {
        var args = new ArrayList<Object>();
        if (request.pathParams() != null) {
            args.addAll(request.pathParams());
        }
        if (request.body() != null) {
            if (request.body() instanceof List<?> list) {
                for (Object item : list) {
                    args.add(item != null ? item.toString() : "");
                }
            } else {
                args.add(request.body().toString());
            }
        }
        return args.isEmpty() ? null : args;
    }

    /**
     * Simple in-memory transport that captures the first message sent to it.
     */
    private static class ResultTransport implements WampTransport {
        private WampMessage captured;

        @Override
        public void send(WampMessage msg) {
            this.captured = msg;
        }

        @Override
        public WampMessage receive() {
            return null;
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        public WampMessage captured() {
            return captured;
        }
    }
}
