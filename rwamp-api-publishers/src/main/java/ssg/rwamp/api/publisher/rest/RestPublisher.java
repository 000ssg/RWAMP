package ssg.rwamp.api.publisher.rest;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.ApiPublisher;
import ssg.rwamp.feature.reflection.ReflectionRegistry;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestResponse;
import ssg.rwamp.rest.RestWampBridge;
import ssg.rwamp.api.publisher.wamp.WampApiPublisher;

import java.util.*;

/**
 * Publishes an {@link ApiDefinition} as REST HTTP endpoints.
 * <p>
 * This publisher:
 * <ol>
 *   <li>Registers WAMP procedures via {@link WampApiPublisher}</li>
 *   <li>Sets up path-to-procedure mappings in the reflection registry</li>
 *   <li>Creates a {@link RestWampBridge} for handling HTTP requests</li>
 * </ol>
 * <p>
 * The publisher resolves REST paths from operation extensions (set by
 * {@code @Operation.path} and {@code @ApiService.path} annotations).
 * <p>
 * Usage:
 * <pre>{@code
 * var publisher = new RestPublisher(router, virtualSessionManager, "com.app")
 *     .basePath("/api");
 * var bridge = publisher.publish(definition);
 * // Now handle HTTP requests through the bridge
 * RestResponse response = bridge.handle(new RestRequest("GET", "/api/users/123", Map.of()));
 * }</pre>
 *
 * @since 0.1.0
 */
public class RestPublisher implements ApiPublisher {

    private final WampRouter router;
    private final VirtualSessionManager virtualSessionManager;
    private final String uriPrefix;
    private String basePath = "";

    public RestPublisher(WampRouter router,
                         VirtualSessionManager virtualSessionManager,
                         String uriPrefix) {
        this.router = Objects.requireNonNull(router);
        this.virtualSessionManager = Objects.requireNonNull(virtualSessionManager);
        this.uriPrefix = Objects.requireNonNull(uriPrefix);
    }

    /** Set the base path prefix for all REST endpoints */
    public RestPublisher basePath(String path) {
        this.basePath = path != null ? path : "";
        return this;
    }

    @Override
    public Object publish(ApiDefinition definition) {
        var wampPublisher = new WampApiPublisher(router, uriPrefix);
        var reflectionRegistry = ReflectionRegistry.createFor(router);

        // Publish to WAMP (registers procedures and reflection metadata)
        wampPublisher.publish(definition);

        // Build path-to-procedure mappings from operations
        var pathMappings = new LinkedHashMap<String, String>();
        for (var group : definition.groups().values()) {
            for (var op : group.operations().values()) {
                String uri = uriPrefix + "." + group.name() + "." + op.name();
                String path = resolvePath(basePath, op);
                if (path != null && !path.isEmpty()) {
                    pathMappings.put(path, uri);
                    
                    // Register in reflection registry for the bridge to discover
                    var meta = new LinkedHashMap<String, Object>();
                    meta.put("type", "procedure");
                    meta.put("rest_path", path);
                    meta.put("group", group.name());
                    reflectionRegistry.define(uri, meta);
                }
            }
        }

        // Create the REST bridge
        var bridge = new RestWampBridge(router, virtualSessionManager, reflectionRegistry);
        
        // Return a result with the bridge and path mappings
        return new RestPublishResult(bridge, pathMappings);
    }

    /**
     * Resolves the REST path for an operation.
     * Checks extensions for path and httpMethods set by the annotation scanner.
     */
    private String resolvePath(String basePath, ApiOperation op) {
        var ext = op.extensions();
        String path = null;
        if (ext != null && ext.get("path") != null) {
            path = (String) ext.get("path");
        }
        if (path == null || path.isEmpty()) {
            path = "/" + op.name();
        }
        
        // Normalize path
        if (!basePath.isEmpty() && !basePath.equals("/")) {
            if (path.startsWith("/")) {
                path = basePath + path;
            } else {
                path = basePath + "/" + path;
            }
        }
        
        return path;
    }

    @Override
    public String type() { return "rest"; }

    /**
     * Result of REST publishing containing the bridge and path mappings.
     */
    public static class RestPublishResult {
        private final RestWampBridge bridge;
        private final Map<String, String> pathMappings;

        RestPublishResult(RestWampBridge bridge, Map<String, String> pathMappings) {
            this.bridge = bridge;
            this.pathMappings = pathMappings;
        }

        /** The REST-WAMP bridge for handling HTTP requests */
        public RestWampBridge bridge() { return bridge; }

        /** Path-to-procedure mappings */
        public Map<String, String> pathMappings() { return Map.copyOf(pathMappings); }

        /** Handle an HTTP request */
        public RestResponse handle(RestRequest request) {
            return bridge.handle(request);
        }
    }
}
