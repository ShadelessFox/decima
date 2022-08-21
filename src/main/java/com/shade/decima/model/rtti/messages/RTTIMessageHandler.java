package com.shade.decima.model.rtti.messages;

import com.shade.decima.model.base.GameType;
import com.shade.util.NotNull;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RTTIMessageHandlers.class)
public @interface RTTIMessageHandler {
    @NotNull
    String type();

    @NotNull
    String message();

    @NotNull
    GameType game();
}
