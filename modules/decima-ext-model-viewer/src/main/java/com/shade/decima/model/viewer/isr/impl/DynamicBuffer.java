package com.shade.decima.model.viewer.isr.impl;

import com.shade.decima.model.viewer.isr.Buffer;
import com.shade.decima.model.viewer.isr.BufferView;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DynamicBuffer implements Buffer {
    private byte[] data;

    public DynamicBuffer(int size) {
        this.data = new byte[size];
    }

    public void grow(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.data = Arrays.copyOf(data, data.length + size);
    }

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
