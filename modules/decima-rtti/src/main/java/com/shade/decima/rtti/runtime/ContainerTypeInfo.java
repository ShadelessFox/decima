package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;

import java.lang.reflect.Type;

public record ContainerTypeInfo(
    @NotNull TypeName.Parameterized name,
    @NotNull Type type,
    @NotNull TypeInfoRef itemType,
    boolean array
) implements TypeInfo {
    @Override
    public String toString() {
        return name.toString();
    }
}
