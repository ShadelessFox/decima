package com.shade.decima.model.rtti.messages;

import com.shade.decima.ui.data.registry.Type;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageHandlerRegistration {
    @NotNull
    String message();

    @NotNull
    Type[] types();
}
