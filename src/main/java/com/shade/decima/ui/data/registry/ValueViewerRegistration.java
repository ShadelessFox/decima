package com.shade.decima.ui.data.registry;

import com.shade.decima.ui.data.ValueViewer;
import com.shade.platform.model.ExtensionPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(ValueViewer.class)
public @interface ValueViewerRegistration {
    Type[] value();
}
