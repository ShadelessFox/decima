package com.shade.decima.ui.data.registry;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.platform.model.ExtensionPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(ValueHandler.class)
public @interface ValueHandlerRegistration {
    Type[] value();

    String id() default "default";

    String name() default "Default";

    int order() default 0;
}
