package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RTTIDefinition {
    @NotNull
    String name();

    @NotNull
    String[] aliases() default {};
}
