package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.rtti.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RTTIExtends {
    Type[] value();
}
