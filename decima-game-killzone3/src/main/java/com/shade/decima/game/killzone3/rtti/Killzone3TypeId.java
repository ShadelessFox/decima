package com.shade.decima.game.killzone3.rtti;

import com.shade.decima.rtti.factory.TypeId;
import com.shade.util.NotNull;

public record Killzone3TypeId(@NotNull String typeName) implements TypeId {
    @NotNull
    public static TypeId of(@NotNull String typeName) {
        return new Killzone3TypeId(typeName);
    }
}
