package com.shade.decima.model.rtti.messages;

import com.shade.decima.model.rtti.Type;
import com.shade.platform.model.ExtensionPoint;
import com.shade.util.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionPoint(MessageHandler.class)
public @interface MessageHandlerRegistration {
    @NotNull
    String message();

    @NotNull
    Type[] types();
}
