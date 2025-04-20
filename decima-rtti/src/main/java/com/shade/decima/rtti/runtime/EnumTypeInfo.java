package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.data.Value;
import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;

public record EnumTypeInfo(
    @NotNull TypeName.Simple name,
    @NotNull Class<? extends Enum<?>> type,
    int size,
    boolean isSet
) implements TypeInfo {
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & Value.OfEnum<T>> Value.OfEnum<T> valueOf(int value) {
        if (isSet) {
            throw new IllegalStateException("Enum " + name + " is a set");
        }
        return Value.valueOf((Class<T>) type, value);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & Value.OfEnumSet<T>> Value.OfEnumSet<T> setOf(int value) {
        if (!isSet) {
            throw new IllegalStateException("Enum " + name + " is not a set");
        }
        return Value.setOf((Class<T>) type, value);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
