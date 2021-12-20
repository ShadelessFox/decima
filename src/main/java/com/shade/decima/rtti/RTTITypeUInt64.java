package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeUInt64 implements RTTIType<Long> {
    @NotNull
    @Override
    public Long read(@NotNull ByteBuffer buffer) {
        return buffer.getLong();
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Long value) {
        buffer.putLong(value);
    }

    @NotNull
    @Override
    public String getName() {
        return "uint64";
    }

    @Override
    public int getSize() {
        return Long.BYTES;
    }
}
