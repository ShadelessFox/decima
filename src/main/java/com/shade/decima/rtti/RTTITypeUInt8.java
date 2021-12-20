package com.shade.decima.rtti;

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

    @Override
    public int getSize() {
        return Byte.BYTES;
    }
}
