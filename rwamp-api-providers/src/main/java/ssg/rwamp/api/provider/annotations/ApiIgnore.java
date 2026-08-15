package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a class, method, field, or parameter from API discovery.
 * <p>
 * Inspired by xLib's {@code exclude = true} on {@code @XMethod}.
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface ApiIgnore {
}
