package ssg.rwamp.api.webservices.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface TestQueryParam {
    String value() default "";
    String description() default "";
    String defaultValue() default "";
}
