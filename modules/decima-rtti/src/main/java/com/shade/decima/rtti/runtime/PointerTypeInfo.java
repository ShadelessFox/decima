package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

import java.lang.reflect.Type;

public record PointerTypeInfo(
    @NotNull TypeName.Parameterized name,
    @NotNull Type type,
    @NotNull TypeInfoRef itemType
) implements TypeInfo {
    @Override
    public String toString() {
        return name.toString();
    }
}
