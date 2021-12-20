package com.shade.decima.rtti;

import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeUInt16 implements RTTIType<Short> {
    @NotNull
    @Override
    public Short read(@NotNull ByteBuffer buffer) {
        return buffer.getShort();
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Short value) {
        buffer.putShort(value);
    }

    @NotNull
    @Override
    public String getName() {
        return "uint16";
    }

    @Override
    public int getSize() {
        return Short.BYTES;
    }
}
