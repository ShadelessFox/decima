package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public interface RTTIType<T> {
    @NotNull
    T read(@NotNull ByteBuffer buffer);

    void write(@NotNull ByteBuffer buffer, @NotNull T value);

    @NotNull
    String getName();

    @NotNull
    Kind getKind();

    @NotNull
    Class<T> getComponentType();

    enum Kind {
        CLASS,
        CONTAINER,
        REFERENCE,
        ENUM,
        ENUM_FLAGS,
        PRIMITIVE
    }
}
