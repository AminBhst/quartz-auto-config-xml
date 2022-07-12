package io.github.aminbhst.quartzautoconfigxml.annotation;


import io.github.aminbhst.quartzautoconfigxml.StoreType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QuartzJob {

    String cron() default "";

    StoreType storeType() default StoreType.IN_MEMORY;

    boolean concurrent() default true;

    String targetMethod() default "execute";
}
