package com.shade.decima.rtti.test;

import com.shade.decima.rtti.factory.TypeId;
import com.shade.util.NotNull;

public record UntilDawnTypeId(@NotNull String typeName) implements TypeId {
    @NotNull
    public static TypeId of(@NotNull String typeName) {
        return new UntilDawnTypeId(typeName);
    }
}
