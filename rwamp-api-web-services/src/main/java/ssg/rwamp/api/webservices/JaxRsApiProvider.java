package ssg.rwamp.api.webservices;

import ssg.rwamp.api.provider.ApiProvider;
import ssg.rwamp.api.provider.model.ApiDefinition;

import java.util.*;

/**
 * API provider for JAX-RS (Java API for RESTful Web Services) annotated classes.
 * <p>
 * Scans classes annotated with {@code @Path} and methods annotated with
 * HTTP method annotations ({@code @GET}, {@code @POST}, {@code @PUT},
 * {@code @DELETE}, {@code @PATCH}, {@code @HEAD}, {@code @OPTIONS}).
 * <p>
 * Supports both Jakarta EE ({@code jakarta.ws.rs.*}) and legacy Java EE
 * ({@code javax.ws.rs.*}) package namespaces. The correct set is auto-detected
 * based on which classes are available on the classpath.
 * <p>
 * <b>Weak dependency:</b> JAX-RS is declared as {@code provided/optional}.
 * The provider dynamically loads annotation classes via {@code Class.forName()}.
 * If JAX-RS is not on the classpath, {@link #isOperable()} returns {@code false}
 * and the provider is effectively disabled without any compilation errors.
 * <p>
 * Usage:
 * <pre>{@code
 * var provider = new JaxRsApiProvider("my-rest-api");
 * if (provider.isOperable()) {
 *     ApiDefinition api = provider.build(MyResource.class);
 *     // expose api via WAMP, OpenAPI, etc.
 * } else {
 *     System.out.println("JAX-RS not available on classpath");
 * }
 * }</pre>
 *
 * @since 0.1.0
 * @see GenericAnnotationApiProvider
 */
public class JaxRsApiProvider implements ApiProvider {

    /** Jakarta EE (9+) namespace */
    public static final String JAKARTA_NS = "jakarta.ws.rs";

    /** Legacy Java EE namespace */
    public static final String JAVAX_NS = "javax.ws.rs";

    private final String apiName;
    private final AnnotationScanner scanner;
    private boolean namespaceDetected = false;
    private String detectedNamespace = null;

    /** Jakarta annotation class names */
    private static final String JAKARTA_PATH = JAKARTA_NS + ".Path";
    private static final String JAKARTA_GET = JAKARTA_NS + ".GET";
    private static final String JAKARTA_POST = JAKARTA_NS + ".POST";
    private static final String JAKARTA_PUT = JAKARTA_NS + ".PUT";
    private static final String JAKARTA_DELETE = JAKARTA_NS + ".DELETE";
    private static final String JAKARTA_PATCH = JAKARTA_NS + ".PATCH";
    private static final String JAKARTA_HEAD = JAKARTA_NS + ".HEAD";
    private static final String JAKARTA_OPTIONS = JAKARTA_NS + ".OPTIONS";
    private static final String JAKARTA_PATH_PARAM = JAKARTA_NS + ".PathParam";
    private static final String JAKARTA_QUERY_PARAM = JAKARTA_NS + ".QueryParam";
    private static final String JAKARTA_HEADER_PARAM = JAKARTA_NS + ".HeaderParam";
    private static final String JAKARTA_FORM_PARAM = JAKARTA_NS + ".FormParam";
    private static final String JAKARTA_COOKIE_PARAM = JAKARTA_NS + ".CookieParam";
    private static final String JAKARTA_CONSUMES = JAKARTA_NS + ".Consumes";
    private static final String JAKARTA_PRODUCES = JAKARTA_NS + ".Produces";

    /** javax annotation class names */
    private static final String JAVAX_PATH = JAVAX_NS + ".Path";
    private static final String JAVAX_GET = JAVAX_NS + ".GET";
    private static final String JAVAX_POST = JAVAX_NS + ".POST";
    private static final String JAVAX_PUT = JAVAX_NS + ".PUT";
    private static final String JAVAX_DELETE = JAVAX_NS + ".DELETE";
    private static final String JAVAX_PATCH = JAVAX_NS + ".PATCH";
    private static final String JAVAX_HEAD = JAVAX_NS + ".HEAD";
    private static final String JAVAX_OPTIONS = JAVAX_NS + ".OPTIONS";
    private static final String JAVAX_PATH_PARAM = JAVAX_NS + ".PathParam";
    private static final String JAVAX_QUERY_PARAM = JAVAX_NS + ".QueryParam";
    private static final String JAVAX_HEADER_PARAM = JAVAX_NS + ".HeaderParam";
    private static final String JAVAX_FORM_PARAM = JAVAX_NS + ".FormParam";
    private static final String JAVAX_COOKIE_PARAM = JAVAX_NS + ".CookieParam";
    private static final String JAVAX_CONSUMES = JAVAX_NS + ".Consumes";
    private static final String JAVAX_PRODUCES = JAVAX_NS + ".Produces";

    public JaxRsApiProvider(String apiName) {
        this(apiName, new AnnotationScanner());
    }

    public JaxRsApiProvider(String apiName, AnnotationScanner scanner) {
        this.apiName = apiName;
        this.scanner = scanner;
    }

    /**
     * Detects the available JAX-RS namespace (Jakarta EE or Java EE).
     * Returns the namespace prefix or {@code null} if neither is available.
     */
    private String detectNamespace() {
        if (namespaceDetected) return detectedNamespace;
        namespaceDetected = true;

        if (scanner.hasAny(JAKARTA_PATH)) {
            detectedNamespace = JAKARTA_NS;
        } else if (scanner.hasAny(JAVAX_PATH)) {
            detectedNamespace = JAVAX_NS;
        } else {
            detectedNamespace = null;
        }
        return detectedNamespace;
    }

    /**
     * Returns the delegate provider configured with the detected namespace.
     * Initializes namespace detection on first call.
     */
    private GenericAnnotationApiProvider delegate() {
        String ns = detectNamespace();
        if (ns == null) return null;

        boolean isJakarta = JAKARTA_NS.equals(ns);
        String path = isJakarta ? JAKARTA_PATH : JAVAX_PATH;
        String get = isJakarta ? JAKARTA_GET : JAVAX_GET;
        String post = isJakarta ? JAKARTA_POST : JAVAX_POST;
        String put = isJakarta ? JAKARTA_PUT : JAVAX_PUT;
        String del = isJakarta ? JAKARTA_DELETE : JAVAX_DELETE;
        String patch = isJakarta ? JAKARTA_PATCH : JAVAX_PATCH;
        String head = isJakarta ? JAKARTA_HEAD : JAVAX_HEAD;
        String options = isJakarta ? JAKARTA_OPTIONS : JAVAX_OPTIONS;

        String pathParam = isJakarta ? JAKARTA_PATH_PARAM : JAVAX_PATH_PARAM;
        String queryParam = isJakarta ? JAKARTA_QUERY_PARAM : JAVAX_QUERY_PARAM;
        String headerParam = isJakarta ? JAKARTA_HEADER_PARAM : JAVAX_HEADER_PARAM;
        String formParam = isJakarta ? JAKARTA_FORM_PARAM : JAVAX_FORM_PARAM;
        String cookieParam = isJakarta ? JAKARTA_COOKIE_PARAM : JAVAX_COOKIE_PARAM;

        String consumes = isJakarta ? JAKARTA_CONSUMES : JAVAX_CONSUMES;
        String produces = isJakarta ? JAKARTA_PRODUCES : JAVAX_PRODUCES;

        var d = new GenericAnnotationApiProvider(apiName, scanner)
                .setTypeAnnotations(path)
                .setMethodAnnotations(get, post, put, del, patch, head, options)
                .setParameterAnnotations(pathParam, queryParam, headerParam, formParam, cookieParam)
                .setConsumesAnnotations(consumes)
                .setProducesAnnotations(produces);

        return d;
    }

    @Override
    public boolean isOperable() {
        return detectNamespace() != null;
    }

    @Override
    public boolean canHandle(Object target) {
        var d = delegate();
        if (d == null) return false;
        return d.canHandle(target);
    }

    @Override
    public ApiDefinition build(Object target) {
        var d = delegate();
        if (d == null) {
            return new ApiDefinition(apiName, "1.0.0", null, Map.of(), Map.of(), Map.of());
        }
        return d.build(target);
    }

    @Override
    public String type() {
        return "jax-rs";
    }

    /**
     * Returns the detected JAX-RS namespace ({@code "jakarta.ws.rs"} or
     * {@code "javax.ws.rs"}) or {@code null} if neither is available.
     */
    public String detectedNamespace() {
        return detectNamespace();
    }
}
