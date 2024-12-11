package com.shade.decima.rtti;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attr {
    String name();

    String type();

    int position();

    int offset();

    int flags() default 0;

    String min() default "";

    String max() default "";
}
