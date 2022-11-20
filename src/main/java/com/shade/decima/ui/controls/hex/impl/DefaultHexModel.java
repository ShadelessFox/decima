package com.shade.decima.ui.controls.hex.impl;

import com.shade.decima.ui.controls.hex.HexModel;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record DefaultHexModel(@NotNull ByteBuffer buffer) implements HexModel {
    public DefaultHexModel(@NotNull byte[] data) {
        this(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
    }

    @Override
    public int getLength() {
        return buffer.capacity();
    }

    @Override
    public byte getByte(int index) {
        return buffer.get(index);
    }

    @Override
    public short getShort(int index) {
        return buffer.getShort(index);
    }

    @Override
    public int getInt(int index) {
        return buffer.getInt(index);
    }

    @Override
    public long getLong(int index) {
        return buffer.getLong(index);
    }

    @Override
    public float getFloat(int index) {
        return buffer.getFloat(index);
    }

    @Override
    public double getDouble(int index) {
        return buffer.getDouble(index);
    }
}
