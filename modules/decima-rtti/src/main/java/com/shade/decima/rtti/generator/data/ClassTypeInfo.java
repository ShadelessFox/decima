package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

import java.util.List;

public record ClassTypeInfo(
    @NotNull String name,
    @NotNull List<ClassBaseInfo> bases,
    @NotNull List<ClassAttrInfo> attrs,
    @NotNull List<String> messages,
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
    public String typeName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
