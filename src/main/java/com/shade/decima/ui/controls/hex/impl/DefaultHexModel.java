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
    public byte get(int index) {
        return buffer.get(index);
    }

    @Override
    public void get(int index, @NotNull byte[] dst, int offset, int length) {
        buffer.get(index, dst, offset, length);
    }

    @Override
    public int length() {
        return buffer.capacity();
    }
}
