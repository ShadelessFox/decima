package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record AtomTypeInfo(
    @NotNull String name,
    @Nullable TypeInfo parent
) implements TypeInfo {
    @NotNull
    @Override
    public TypeName typeName() {
        return TypeName.of(name);
    }
}
