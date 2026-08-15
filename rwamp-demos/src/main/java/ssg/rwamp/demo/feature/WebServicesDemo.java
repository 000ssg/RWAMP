package ssg.rwamp.demo.feature;

import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.provider.model.ApiGroup;
import ssg.rwamp.api.provider.model.ApiOperation;
import ssg.rwamp.api.webservices.GenericAnnotationApiProvider;
import ssg.rwamp.api.webservices.JaxRsApiProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo of dynamic web-service annotation scanning (xLib-inspired weak dependency pattern).
 * <p>
 * This demo shows how RWAMP discovers API operations from web-service annotations
 * without requiring compile-time dependencies on the annotation libraries:
 * <ol>
 *   <li>Generic provider — user-configured annotation class names</li>
 *   <li>JAX-RS provider — auto-detects Jakarta EE vs Java EE namespace</li>
 *   <li>Graceful degradation — provider reports as non-operable when annotations are absent</li>
 * </ol>
 * <p>
 * The demo uses simple test annotations to simulate JAX-RS-like patterns.
 *
 * @since 0.1.0
 */
public class WebServicesDemo {

    /**
     * Sample REST resource annotated with test annotations (simulates JAX-RS).
     */
    @TestPath("/api/users")
    public static class UserResource {

        @TestGet
        public List<String> listAll() {
            return List.of("alice", "bob");
        }

        @TestGet
        @TestPath("/{id}")
        public String getById(@TestPathParam("id") String id) {
            return "user:" + id;
        }

        @TestPost
        @TestPath("/create")
        @TestConsumes({"application/json"})
        public String create(@TestQueryParam("name") String name) {
            return "created:" + name;
        }
    }

    /**
     * Sample resource with no web-service annotations.
     */
    public static class PlainService {
        public void doSomething(String input) {
            // internal method — no annotations
        }
    }

    // ── Test annotations (simulate JAX-RS without the library) ──

    private static final String TEST_PATH = "ssg.rwamp.demo.feature.WebServicesDemo$TestPath";
    private static final String TEST_GET = "ssg.rwamp.demo.feature.WebServicesDemo$TestGet";
    private static final String TEST_POST = "ssg.rwamp.demo.feature.WebServicesDemo$TestPost";
    private static final String TEST_PATH_PARAM = "ssg.rwamp.demo.feature.WebServicesDemo$TestPathParam";
    private static final String TEST_QUERY_PARAM = "ssg.rwamp.demo.feature.WebServicesDemo$TestQueryParam";
    private static final String TEST_CONSUMES = "ssg.rwamp.demo.feature.WebServicesDemo$TestConsumes";

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Documented
    public @interface TestPath { String value() default ""; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @java.lang.annotation.Documented
    public @interface TestGet { String value() default ""; String description() default ""; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @java.lang.annotation.Documented
    public @interface TestPost { String value() default ""; String description() default ""; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
    @java.lang.annotation.Documented
    public @interface TestPathParam { String value() default ""; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
    @java.lang.annotation.Documented
    public @interface TestQueryParam { String value() default ""; String defaultValue() default "__NOT_SET"; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
    @java.lang.annotation.Documented
    public @interface TestConsumes { String[] value() default {}; }

    public record DemoResult(
            String genericResult,
            String jaxRsResult,
            String degradationResult,
            String unannotatedResult
    ) {}

    /**
     * Runs the demo and returns a summary of results.
     */
    public DemoResult run() {
        // 1. Generic annotation provider
        var genericProvider = new GenericAnnotationApiProvider("user-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM)
                .setConsumesAnnotations(TEST_CONSUMES);

        ApiDefinition genericApi = genericProvider.build(UserResource.class);
        String genericResult = summarize("generic-annotations", genericApi);

        // 2. JAX-RS provider (auto-detect namespace)
        var jaxRsProvider = new JaxRsApiProvider("jaxrs-api");
        String jaxRsResult;
        if (jaxRsProvider.isOperable()) {
            jaxRsResult = "JAX-RS detected (" + jaxRsProvider.detectedNamespace()
                    + ") — operable with " + jaxRsProvider.type();
        } else {
            jaxRsResult = "JAX-RS not on classpath — gracefully disabled (weak dependency)";
        }

        // 3. Graceful degradation — non-existent annotations
        var degradedProvider = new GenericAnnotationApiProvider("degraded-api")
                .setTypeAnnotations("com.nonexistent.FakeAnnotation")
                .setMethodAnnotations("com.nonexistent.FakeMethod");
        String degradationResult = degradedProvider.isOperable()
                ? "UNEXPECTED: provider is operable"
                : "Provider correctly reports as non-operable (annotations absent)";

        // 4. Unannotated class
        ApiDefinition plainApi = genericProvider.build(PlainService.class);
        String unannotatedResult = plainApi.allOperations().isEmpty()
                ? "Plain service correctly skipped (no annotations)"
                : "UNEXPECTED: found operations on plain class";

        return new DemoResult(genericResult, jaxRsResult, degradationResult, unannotatedResult);
    }

    private String summarize(String provider, ApiDefinition api) {
        var summary = new LinkedHashMap<String, Integer>();
        for (var entry : api.groups().entrySet()) {
            ApiGroup group = entry.getValue();
            int opCount = group.allOperations().size();
            summary.put(entry.getKey(), opCount);

            for (ApiOperation op : group.allOperations()) {
                var ext = op.extensions();
                if (ext.containsKey("httpMethod")) {
                    // This is a web-service operation
                }
            }
        }
        return provider + ": groups=" + summary + ", totalOps=" + api.allOperations().size();
    }

    public static void main(String[] args) {
        var demo = new WebServicesDemo();
        var result = demo.run();
        System.out.println("WebServicesDemo:");
        System.out.println("  Generic provider: " + result.genericResult());
        System.out.println("  JAX-RS provider: " + result.jaxRsResult());
        System.out.println("  Degradation: " + result.degradationResult());
        System.out.println("  Unannotated: " + result.unannotatedResult());
    }
}
