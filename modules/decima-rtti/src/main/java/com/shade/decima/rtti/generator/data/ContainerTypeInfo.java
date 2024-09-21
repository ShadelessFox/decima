package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public record ContainerTypeInfo(
    @NotNull String name,
    @NotNull TypeInfoRef type
) implements TypeInfo {
    @NotNull
    @Override
    public String fullName() {
        return "%s<%s>".formatted(name, type.typeName());
    }
}
