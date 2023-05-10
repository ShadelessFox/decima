package com.shade.decima.ui.data.registry;

import com.shade.decima.model.rtti.Type;
import com.shade.decima.ui.data.ValueManager;
import com.shade.platform.model.ExtensionPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(ValueManager.class)
public @interface ValueManagerRegistration {
    Type[] value();
}
