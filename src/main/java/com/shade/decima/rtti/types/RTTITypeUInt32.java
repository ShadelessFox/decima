package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeUInt32 implements RTTIType<Integer> {
    @NotNull
    @Override
    public Integer read(@NotNull ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Integer value) {
        buffer.putInt(value);
    }

    @NotNull
    @Override
    public String getName() {
        return "uint32";
    }

    @NotNull
    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public int getSize() {
        return Integer.BYTES;
    }
}
