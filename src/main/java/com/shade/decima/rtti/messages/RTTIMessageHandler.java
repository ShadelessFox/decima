package com.shade.decima.rtti.messages;

import com.shade.decima.base.GameType;
import com.shade.decima.util.NotNull;

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
