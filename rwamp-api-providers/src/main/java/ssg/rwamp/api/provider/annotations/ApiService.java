package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an API service whose public methods should be exposed.
 * <p>
 * Inspired by xLib's {@code @XType} / JAX-RS {@code @Path}.
 * <p>
 * Example:
 * <pre>{@code
 * @ApiService(name = "users", path = "/api/users", tags = {"admin"})
 * public class UserService {
 *     @ApiOperation(summary = "Get user by ID")
 *     public User getUser(@ApiParam(name = "id") Long id) { ... }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiService {

    /** Display name for the API group (defaults to simple class name) */
    String name() default "";

    /** Human-readable description (defaults to empty) */
    String description() default "";

    /** REST path prefix for this service (e.g. "/api/users") */
    String path() default "";

    /** Logical grouping tags */
    String[] tags() default {};

    /** Role-based access control constraints */
    String[] roles() default {};
}
