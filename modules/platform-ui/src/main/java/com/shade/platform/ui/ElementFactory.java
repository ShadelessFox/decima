package com.shade.platform.ui;

import com.shade.platform.model.ExtensionPoint;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.prefs.Preferences;

public interface ElementFactory {
    @NotNull
    SaveableElement createElement(@NotNull Preferences pref);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ExtensionPoint(ElementFactory.class)
    @interface Registration {
        String value();
    }
}
