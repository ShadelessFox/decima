package com.shade.decima.rtti;

import com.shade.decima.rtti.runtime.ClassTypeInfo;
import com.shade.util.NotNull;

public interface TypeFactory {
    @NotNull
    ClassTypeInfo get(@NotNull TypeId id);

    @NotNull
    ClassTypeInfo get(@NotNull Class<?> cls);

    @NotNull
    default <T> T newInstance(@NotNull Class<T> cls) {
        return cls.cast(get(cls).newInstance());
    }
}
