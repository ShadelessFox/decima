package com.shade.decima.ui.action;

import com.shade.decima.model.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActionContribution {
    int SEPARATOR_BEFORE = 1;
    int SEPARATOR_AFTER = 1 << 1;

    @NotNull
    String path();

    int position() default Integer.MAX_VALUE;

    int separator() default 0;
}
