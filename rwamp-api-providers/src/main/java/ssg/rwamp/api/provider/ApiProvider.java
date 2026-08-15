package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;

/**
 * Strategy interface for discovering and building API definitions.
 * <p>
 * Providers analyze source (Java classes, annotations, configurations) and
 * produce a structured {@link ApiDefinition} that publishers can then expose
 * as OpenAPI specs, WAMP procedures, or interactive documentation.
 * <p>
 * Inspired by xLib's {@code MethodsProvider} and {@code Reflective_API_Builder}.
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@code AnnotationBasedApiProvider} — scans {@code @ApiService} annotated classes</li>
 *   <li>{@code GetterSetterApiProvider} — derives operations from JavaBean getter/setter pairs</li>
 *   <li>{@code ManualApiProvider} — programmatic builder for custom definitions</li>
 * </ul>

 * @since 0.1.0
 */
public interface ApiProvider {

    /**
     * Returns true if this provider is operable — i.e., its required
     * dependencies are available and it can process targets.
     * <p>
     * Defaults to {@code true}. Override to implement a classpath guard
     * (e.g. for providers that rely on optional annotation libraries).
     *
     * @return true if this provider is ready to handle targets
     */
    default boolean isOperable() {
        return true;
    }

    /**
     * Returns true if this provider can handle the given target.
     *
     * @param target a {@code Class}, object instance, or configuration map
     * @return true if this provider can process the target
     */
    boolean canHandle(Object target);

    /**
     * Builds an API definition from the given target.
     * <p>
     * May use reflection, annotations, or internal state depending on
     * the provider implementation.
     *
     * @param target a {@code Class}, object instance, or configuration map
     * @return the API definition (never null, may be empty)
     */
    ApiDefinition build(Object target);

    /**
     * Optional provider identifier for debugging and diagnostics.
     *
     * @return a short name (e.g. "annotations", "bean", "manual")
     */
    default String type() {
        return getClass().getSimpleName();
    }
}
