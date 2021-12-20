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
    Class<T> getType();

    int getSize();
}
