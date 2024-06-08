package com.shade.decima.model.rtti.gen;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface TypeAdapter<T> {
    @NotNull
    T read(@NotNull ByteBuffer buffer);

    void write(@NotNull ByteBuffer buffer, @NotNull T value);
}
