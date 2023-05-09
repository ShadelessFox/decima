package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.Type;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RTTIField {
    @NotNull
    Type type();

    @NotNull
    String name() default "";
}
