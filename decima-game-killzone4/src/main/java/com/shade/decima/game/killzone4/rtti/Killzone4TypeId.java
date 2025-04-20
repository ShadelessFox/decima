package com.shade.decima.game.killzone4.rtti;

import com.shade.decima.rtti.factory.TypeId;
import com.shade.util.NotNull;

public record Killzone4TypeId(@NotNull String typeName) implements TypeId {
    @NotNull
    public static TypeId of(@NotNull String typeName) {
        return new Killzone4TypeId(typeName);
    }
}
