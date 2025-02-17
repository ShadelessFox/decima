package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

import java.util.List;

public record ClassTypeInfo(
    @NotNull TypeName.Simple name,
    @NotNull Class<?> interfaceType,
    @NotNull Class<?> instanceType,
    @NotNull List<ClassBaseInfo> bases,
    @NotNull List<ClassAttrInfo> displayableAttrs,
    @NotNull List<ClassAttrInfo> serializableAttrs
) implements TypeInfo {
    @NotNull
    @SuppressWarnings("deprecation")
    public Object newInstance() {
        try {
            return instanceType.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create instance of " + name, e);
        }
    }

    @NotNull
    @Override
    public Class<?> type() {
        return interfaceType;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
