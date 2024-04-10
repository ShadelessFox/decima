package com.shade.decima.model.rtti;

import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public interface RTTIBinaryReader {
    /**
     * Reads a type from the buffer.
     * <p>
     * A type must not use method to read itself; instead, it should use it
     * to read other types, e.g. class attributes or container elements.
     */
    @NotNull
    <T> T read(@NotNull RTTIType<T> type, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer);
}
