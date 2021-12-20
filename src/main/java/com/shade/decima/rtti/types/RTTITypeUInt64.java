package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
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

    @NotNull
    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public int getSize() {
        return Long.BYTES;
    }
}
