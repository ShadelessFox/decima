package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public record AtomTypeInfo(
    @NotNull TypeName.Simple name,
    @NotNull Class<?> type
) implements TypeInfo {
    @Override
    public String toString() {
        return name.toString();
    }
}
