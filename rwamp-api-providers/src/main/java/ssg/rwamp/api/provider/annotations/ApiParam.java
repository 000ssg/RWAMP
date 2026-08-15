package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a method parameter for API documentation and routing.
 * <p>
 * Inspired by xLib's {@code @XParameter} and Spring's {@code @RequestParam}.
 * <p>
 * Example:
 * <pre>{@code
 * public User getUser(@ApiParam(name = "userId", required = true) Long id) { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ApiParam {

    /** Parameter name (defaults to the Java parameter name) */
    String name() default "";

    /** Human-readable description */
    String description() default "";

    /** Whether the parameter is mandatory */
    boolean required() default true;

    /** Default value when not provided (Java literal syntax) */
    String defaultValue() default "";

    /** Where the value is sourced from */
    In in() default In.AUTO;

    /** Optional grouping tags */
    String[] tags() default {};
}
