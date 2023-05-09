package com.shade.platform.ui.settings;

import com.shade.platform.model.ExtensionPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(SettingsPage.class)
public @interface SettingsPageRegistration {
    String parent() default "";

    String id();

    String name();
}
