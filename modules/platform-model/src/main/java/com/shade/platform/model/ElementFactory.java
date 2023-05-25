package com.shade.platform.model;

import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

public interface ElementFactory {
    @NotNull
    SaveableElement createElement(@NotNull Map<String, Object> state);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ExtensionPoint(ElementFactory.class)
    @interface Registration {
        String value();
    }
}
