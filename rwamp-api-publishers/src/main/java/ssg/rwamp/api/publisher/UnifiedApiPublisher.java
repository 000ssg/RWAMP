package ssg.rwamp.api.publisher;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.ApiProvider;
import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.publisher.openapi.OpenApiPublisher;
import ssg.rwamp.api.publisher.wamp.WampApiPublisher;
import ssg.rwamp.api.publisher.jsdoc.JsDocPublisher;
import ssg.rwamp.api.publisher.rest.RestPublisher;
import ssg.rwamp.feature.reflection.ReflectionRegistry;
import ssg.rwamp.feature.virtual.VirtualSessionManager;

import java.util.*;

/**
 * Orchestrates API discovery (via providers) and publishing (via publishers).
 * <p>
 * Combines the provider → definition → publisher pipeline into a single
 * high-level API. Supports multiple publishers simultaneously:
 * <ul>
 *   <li>OpenAPI spec generation (JSON/YAML)</li>
 *   <li>WAMP procedure registration</li>
 *   <li>REST HTTP endpoint exposure</li>
 *   <li>Interactive HTML documentation</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * var publisher = new UnifiedApiPublisher("com.app.api")
 *     .provider(new AnnotationBasedApiProvider("my-api"))
 *     .openApiPublisher(p -> p
 *         .serverUrl("https://api.example.com")
 *         .license("MIT"))
 *     .wampPublisher(router, "com.app")
 *     .restPublisher(router, sessionManager, "com.app")
 *         .basePath("/api")
 *     .jsDocPublisher(p -> p.title("My API Docs"));
 *
 * var results = publisher.publish(UserService.class, OrderService.class);
 * String openApiJson = results.openApiJson();
 * String htmlDoc = results.jsDocHtml();
 * var restBridge = results.restBridge();
 * }</pre>
 *
 * @since 0.1.0
 */
public class UnifiedApiPublisher {

    private final String name;
    private final List<ApiProvider> providers = new ArrayList<>();
    private OpenApiPublisher openApiPublisher;
    private WampApiPublisher wampPublisher;
    private RestPublisher restPublisher;
    private JsDocPublisher jsDocPublisher;
    private String version = "1.0.0";

    public UnifiedApiPublisher(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public UnifiedApiPublisher version(String v) { this.version = v; return this; }
    
    public UnifiedApiPublisher provider(ApiProvider provider) {
        if (provider != null) providers.add(provider);
        return this;
    }

    /** Configure the OpenAPI publisher. Returns this for chaining. */
    public UnifiedApiPublisher openApiPublisher() {
        if (openApiPublisher == null) openApiPublisher = new OpenApiPublisher();
        return this;
    }
    
    /** Configure the OpenAPI publisher with a consumer for fine-grained settings. */
    public UnifiedApiPublisher openApiPublisher(java.util.function.Consumer<OpenApiPublisher> config) {
        if (openApiPublisher == null) openApiPublisher = new OpenApiPublisher();
        config.accept(openApiPublisher);
        return this;
    }

    /** Configure the WAMP publisher. Returns this for chaining. */
    public UnifiedApiPublisher wampPublisher(WampRouter router, String uriPrefix) {
        if (wampPublisher == null) {
            wampPublisher = new WampApiPublisher(router, uriPrefix);
        }
        return this;
    }
    
    /** Configure the WAMP publisher with a consumer for fine-grained settings. */
    public UnifiedApiPublisher wampPublisher(WampRouter router, String uriPrefix,
                                              java.util.function.Consumer<WampApiPublisher> config) {
        if (wampPublisher == null) {
            wampPublisher = new WampApiPublisher(router, uriPrefix);
        }
        config.accept(wampPublisher);
        return this;
    }

    /** Configure the REST publisher. Returns this for chaining. */
    public UnifiedApiPublisher restPublisher(WampRouter router,
                                              VirtualSessionManager sessionManager,
                                              String uriPrefix) {
        if (restPublisher == null) {
            restPublisher = new RestPublisher(router, sessionManager, uriPrefix);
        }
        return this;
    }
    
    /** Configure the REST publisher with a consumer for fine-grained settings. */
    public UnifiedApiPublisher restPublisher(WampRouter router,
                                              VirtualSessionManager sessionManager,
                                              String uriPrefix,
                                              java.util.function.Consumer<RestPublisher> config) {
        if (restPublisher == null) {
            restPublisher = new RestPublisher(router, sessionManager, uriPrefix);
        }
        config.accept(restPublisher);
        return this;
    }

    /** Configure the JS documentation publisher. Returns this for chaining. */
    public UnifiedApiPublisher jsDocPublisher() {
        if (jsDocPublisher == null) jsDocPublisher = new JsDocPublisher();
        return this;
    }
    
    /** Configure the JS documentation publisher with a consumer for fine-grained settings. */
    public UnifiedApiPublisher jsDocPublisher(java.util.function.Consumer<JsDocPublisher> config) {
        if (jsDocPublisher == null) jsDocPublisher = new JsDocPublisher();
        config.accept(jsDocPublisher);
        return this;
    }

    /**
     * Publishes from the configured providers and targets.
     *
     * @param targets classes or objects to scan
     * @return the publish results
     */
    public PublishResult publish(Object... targets) {
        ApiDefinition apiDef = buildDefinition(targets);

        var result = new PublishResult(apiDef);

        if (openApiPublisher != null) {
            result.openApiJson = openApiPublisher.publishAsJson(apiDef);
            result.openApiYaml = openApiPublisher.publishAsYaml(apiDef);
        }
        if (wampPublisher != null) {
            result.wampResults = (Map<String, String>) wampPublisher.publish(apiDef);
        }
        if (restPublisher != null) {
            var restResult = (RestPublisher.RestPublishResult) restPublisher.publish(apiDef);
            result.restBridge = restResult.bridge();
            result.restPathMappings = restResult.pathMappings();
        }
        if (jsDocPublisher != null) {
            result.jsDocHtml = (String) jsDocPublisher.publish(apiDef);
        }

        return result;
    }

    private ApiDefinition buildDefinition(Object[] targets) {
        var groups = new LinkedHashMap<String, ssg.rwamp.api.provider.model.ApiGroup>();
        var types = new LinkedHashMap<String, ssg.rwamp.api.provider.model.ApiDataType>();

        for (var target : targets) {
            for (var provider : providers) {
                if (!provider.canHandle(target)) continue;
                var def = provider.build(target);
                groups.putAll(def.groups());
                types.putAll(def.types());
            }
        }

        return new ApiDefinition(name, version, null, groups, types, Map.of());
    }

    /**
     * Result container for unified publishing.
     */
    public static class PublishResult {
        private final ApiDefinition definition;
        private String openApiJson;
        private String openApiYaml;
        private Map<String, String> wampResults;
        private ssg.rwamp.rest.RestWampBridge restBridge;
        private Map<String, String> restPathMappings;
        private String jsDocHtml;

        PublishResult(ApiDefinition def) { this.definition = def; }

        public ApiDefinition definition() { return definition; }
        public String openApiJson() { return openApiJson; }
        public String openApiYaml() { return openApiYaml; }
        public Map<String, String> wampResults() { return wampResults; }
        public ssg.rwamp.rest.RestWampBridge restBridge() { return restBridge; }
        public Map<String, String> restPathMappings() { return restPathMappings; }
        public String jsDocHtml() { return jsDocHtml; }
    }
}
