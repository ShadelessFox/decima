package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public record PointerTypeInfo(
    @NotNull TypeName.Parameterized name,
    @NotNull Class<?> type,
    @NotNull TypeInfoRef itemType
) implements TypeInfo {
    @Override
    public String toString() {
        return name.toString();
    }
}
