package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a field or getter/setter property for API documentation.
 * <p>
 * Inspired by xLib's property-based API discovery. Used when deriving
 * data type schemas from JavaBean classes.

 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface ApiProperty {

    /** Override the default property name */
    String name() default "";

    /** Human-readable description */
    String description() default "";

    /** Whether this property is required */
    boolean required() default false;

    /** Whether null values are allowed */
    boolean nullable() default true;

    /** Example value */
    String example() default "";

    /** Exclude this property from API exposure */
    boolean hidden() default false;
}
