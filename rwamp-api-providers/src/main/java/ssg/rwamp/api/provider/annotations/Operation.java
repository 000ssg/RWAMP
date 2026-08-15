package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as an exposed API operation.
 * <p>
 * Inspired by xLib's {@code @XMethod} and OpenAPI's {@code @Operation}.
 * Without this annotation, public methods are still picked up by convention
 * (see {@code GetterSetterApiProvider}).

 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Operation {

    /** Operation name (defaults to method name converted to kebab-case) */
    String name() default "";

    /** Short one-line summary */
    String summary() default "";

    /** Detailed description */
    String description() default "";

    /** Stable identifier for code generation (defaults to method name) */
    String operationId() default "";

    /** REST path segment (appended to class {@code @ApiService.path}) */
    String path() default "";

    /** HTTP methods this operation responds to (ignored for WAMP-only APIs) */
    HttpMethod[] httpMethods() default {};

    /** Logical grouping tags */
    String[] tags() default {};

    /** Role-based access control constraints */
    String[] roles() default {};

    /** Mark this operation as deprecated */
    boolean deprecated() default false;

    /** Hide from auto-generated documentation */
    boolean hidden() default false;

    /** Explicitly exclude this method from API exposure */
    boolean exclude() default false;
}
