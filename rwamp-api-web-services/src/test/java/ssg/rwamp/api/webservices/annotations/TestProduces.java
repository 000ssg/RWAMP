package ssg.rwamp.api.webservices.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface TestProduces {
    String[] value() default {};
}
