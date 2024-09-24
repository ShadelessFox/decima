package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;

import java.util.List;

public record EnumTypeInfo(
    @NotNull String name,
    @NotNull List<EnumValueInfo> values,
    @NotNull EnumValueSize size,
    boolean flags
) implements TypeInfo {
    @NotNull
    @Override
    public TypeName typeName() {
        return TypeName.of(name);
    }
}
