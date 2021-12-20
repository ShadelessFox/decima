package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeUInt8 implements RTTIType<Byte> {
    @NotNull
    @Override
    public Byte read(@NotNull ByteBuffer buffer) {
        return buffer.get();
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Byte value) {
        buffer.put(value);
    }

    @NotNull
    @Override
    public String getName() {
        return "uint8";
    }

    @NotNull
    @Override
    public Class<Byte> getType() {
        return Byte.class;
    }

    @Override
    public int getSize() {
        return Byte.BYTES;
    }
}
