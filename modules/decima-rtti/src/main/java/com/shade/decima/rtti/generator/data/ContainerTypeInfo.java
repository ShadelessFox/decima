package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;

public record ContainerTypeInfo(
    @NotNull String name,
    @NotNull TypeInfoRef type
) implements TypeInfo {
    @NotNull
    @Override
    public TypeName typeName() {
        return TypeName.of(name, type.typeName());
    }
}
