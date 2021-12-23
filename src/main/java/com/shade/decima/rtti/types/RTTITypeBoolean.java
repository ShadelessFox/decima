package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeBoolean implements RTTIType<Boolean> {
    @NotNull
    @Override
    public Boolean read(@NotNull ByteBuffer buffer) {
        return buffer.get() > 0;
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Boolean value) {
        buffer.put(value == Boolean.TRUE ? (byte) 1 : (byte) 0);
    }

    @NotNull
    @Override
    public String getName() {
        return "bool";
    }

    @NotNull
    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public int getSize() {
        return Byte.BYTES;
    }
}
