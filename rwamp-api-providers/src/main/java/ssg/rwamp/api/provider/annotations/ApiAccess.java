package ssg.rwamp.api.provider.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares access control constraints for a class, method, or parameter.
 * <p>
 * Inspired by xLib's {@code @XAccess}. Supports role-based access and
 * action-level permissions.
 * <p>
 * Annotations are inherited from class-level to method-level with merging;
 * method-level annotations override class-level when both are present.
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface ApiAccess {

    /** Roles permitted to access */
    String[] roles() default {};

    /** Fine-grained actions (e.g. "read", "write", "admin") */
    String[] actions() default {};

    /** Tags for grouping access rules */
    String[] tags() default {};
}
