package com.shade.decima.ui.data.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValueHandlerRegistration {
    Type[] value();

    String id() default "default";

    String name() default "Default";

    int order() default 0;
}
