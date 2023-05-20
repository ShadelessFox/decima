package com.shade.decima.ui.data.registry;

import com.shade.decima.model.base.GameType;
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
    String id() default "default";

    String name() default "Default";

    int order() default 0;

    Selector[] value();

    @interface Selector {
        Field field() default @Field(type = "", field = "");

        Type type() default @Type();

        GameType[] game() default {};
    }

    @interface Type {
        String name() default "";

        Class<?> type() default Void.class;
    }

    @interface Field {
        String type();

        String field();
    }
}
