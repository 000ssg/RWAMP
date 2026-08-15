package ssg.rwamp.api.webservices.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface TestGet {
    String value() default "";
    String description() default "";
}
