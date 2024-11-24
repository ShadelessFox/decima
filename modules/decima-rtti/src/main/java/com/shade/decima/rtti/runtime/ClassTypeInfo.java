package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

import java.util.List;

public record ClassTypeInfo(
    @NotNull TypeName.Simple name,
    @NotNull Class<?> type,
    @NotNull List<ClassBaseInfo> bases,
    @NotNull List<ClassAttrInfo> declaredAttrs,
    @NotNull List<ClassAttrInfo> serializableAttrs
) implements TypeInfo {
    @NotNull
    @SuppressWarnings({"deprecation"})
    public Object newInstance() {
        try {
            // The result of this method is called, and we guarantee that it won't throw any checked exceptions.
            return type.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create instance of " + name, e);
        }
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
