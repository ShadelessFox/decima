package com.shade.decima.ui.data.viewer.texture.util;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Objects;

public class BitBuffer {
    private final byte[] data;
    private int position;

    public BitBuffer(@NotNull byte[] data) {
        this.data = data;
    }

    public BitBuffer(@NotNull ByteBuffer buffer, int index, int length) {
        this(getBytes(buffer, index, length));
    }

    public BitBuffer(@NotNull ByteBuffer buffer, int length) {
        this(buffer, buffer.position(), length);
    }

    public int get(int count) {
        final int bits = get(position, count);
        position += count;
        return bits;
    }

    public int get(int position, int count) {
        Objects.checkFromIndexSize(position, count, length());

        final int idx = position / 8;
        final int bit = position % 8;
        final int chunk;

        if (bit + count <= 8) {
            chunk = data[idx];
        } else {
            chunk = (data[idx] & 0xff) | data[idx + 1] << 8;
        }

        return chunk >>> bit & (1 << count) - 1;
    }

    public int position() {
        return position;
    }

    public int length() {
        return data.length * 8;
    }

    @Override
    public String toString() {
        return "BitBuffer[pos=" + position + " len=" + length() + "]";
    }

    @NotNull
    private static byte[] getBytes(@NotNull ByteBuffer buffer, int index, int length) {
        final byte[] data = new byte[length];
        buffer.get(index, data, 0, length);
        buffer.position(index + length);
        return data;
    }
}
