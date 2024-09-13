package com.shade.decima.rtti.data;

import com.shade.util.NotNull;

import java.util.List;

public record EnumTypeInfo(
    @NotNull String name,
    @NotNull List<EnumValueInfo> values,
    @NotNull EnumValueSize size,
    boolean isBitSet
) implements TypeInfo {
    @NotNull
    @Override
    public String typeName() {
        return name;
    }
}
