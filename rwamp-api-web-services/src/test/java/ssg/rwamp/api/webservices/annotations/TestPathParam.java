package ssg.rwamp.api.webservices.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface TestPathParam {
    String value() default "";
    String description() default "";
}
