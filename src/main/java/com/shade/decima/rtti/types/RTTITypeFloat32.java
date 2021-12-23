package com.shade.decima.rtti.types;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;

import java.nio.ByteBuffer;

public class RTTITypeFloat32 implements RTTIType<Float> {
    @NotNull
    @Override
    public Float read(@NotNull ByteBuffer buffer) {
        return buffer.getFloat();
    }

    @Override
    public void write(@NotNull ByteBuffer buffer, @NotNull Float value) {
        buffer.putFloat(value);
    }

    @NotNull
    @Override
    public String getName() {
        return "float";
    }

    @NotNull
    @Override
    public Class<Float> getType() {
        return Float.class;
    }

    @Override
    public int getSize() {
        return Float.BYTES;
    }
}
