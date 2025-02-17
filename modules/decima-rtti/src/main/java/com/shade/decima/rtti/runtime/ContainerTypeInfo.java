package com.shade.decima.rtti.runtime;

import com.shade.decima.rtti.factory.TypeName;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.MethodHandle;

public record ContainerTypeInfo(
    @NotNull TypeName.Parameterized name,
    @NotNull Class<?> type,
    @NotNull TypeInfoRef itemType,
    @NotNull MethodHandle getter,
    @NotNull MethodHandle setter,
    @NotNull MethodHandle length
) implements TypeInfo {
    @Override
    public String toString() {
        return name.toString();
    }

    @Nullable
    public Object get(@NotNull Object container, int index) {
        try {
            return getter.invoke(container, index);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to get element from " + name, e);
        }
    }

    public void set(@NotNull Object container, int index, @Nullable Object value) {
        try {
            setter.invoke(container, index, value);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to set element in " + name, e);
        }
    }

    public int length(@NotNull Object container) {
        try {
            return (int) length.invoke(container);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to get length of " + name, e);
        }
    }
}
