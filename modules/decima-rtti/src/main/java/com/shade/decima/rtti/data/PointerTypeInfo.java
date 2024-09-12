package com.shade.decima.rtti.data;

import com.shade.util.NotNull;

public record PointerTypeInfo(
    @NotNull String name,
    @NotNull TypeInfoRef type
) implements TypeInfo {
    @NotNull
    @Override
    public String typeName() {
        return "%s<%s>".formatted(name, type.typeName());
    }
}
