package com.shade.decima.rtti.data;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record AtomTypeInfo(
    @NotNull String name,
    @Nullable TypeInfo parent
) implements TypeInfo {
    @NotNull
    @Override
    public String typeName() {
        return name;
    }
}
