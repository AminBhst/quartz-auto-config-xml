package io.github.aminbhst.quartzautoconfigxml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConstructorArg {

    String ref() default "";

    String index() default "";

    String name() default "";

    String type() default "";

    String value() default "";
}
