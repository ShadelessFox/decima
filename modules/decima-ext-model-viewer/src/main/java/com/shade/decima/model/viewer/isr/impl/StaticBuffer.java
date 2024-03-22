package com.shade.decima.model.viewer.isr.impl;

import com.shade.decima.model.viewer.isr.Buffer;
import com.shade.decima.model.viewer.isr.BufferView;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record StaticBuffer(@NotNull byte[] data) implements Buffer {
    @NotNull
    @Override
    public BufferView asView(int offset, int length) {
        return new BufferView(this, offset, length);
    }

    @NotNull
    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer
            .wrap(data)
            .order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int length() {
        return data.length;
    }
}
