package com.shade.decima.model.rtti;

import com.shade.decima.model.base.GameType;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RTTIDefinition {
    @NotNull
    String[] value();

    @NotNull
    GameType[] game() default {};
}
