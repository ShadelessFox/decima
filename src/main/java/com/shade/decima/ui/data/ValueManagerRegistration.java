package com.shade.decima.ui.data;

import com.shade.decima.model.rtti.RTTIType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValueManagerRegistration {
    String[] names() default {};

    Class<? extends RTTIType>[] types() default {};
}
