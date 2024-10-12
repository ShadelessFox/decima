package com.shade.decima.rtti.generator.data;

import com.shade.decima.rtti.TypeName;
import com.shade.util.NotNull;

import java.util.List;
import java.util.Set;

public record ClassTypeInfo(
    @NotNull String name,
    @NotNull List<ClassBaseInfo> bases,
    @NotNull List<ClassAttrInfo> attrs,
    @NotNull Set<String> messages,
    int version,
    int flags
) implements TypeInfo {
    public boolean isAssignableTo(@NotNull String name) {
        if (name().equals(name)) {
            return true;
        }
        for (ClassBaseInfo base : bases) {
            if (base.type().isAssignableTo(name)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public TypeName typeName() {
        return TypeName.of(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
