package com.shade.util.io;

import com.shade.util.ArrayUtils;

import java.nio.ByteOrder;
import java.util.Objects;

final class ByteArrayBinaryReader implements BinaryReader {
    private final byte[] array;
    private final int offset;
    private final int length;
    private int position;
    private ByteOrder order = ByteOrder.LITTLE_ENDIAN;

    ByteArrayBinaryReader(byte[] array, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public byte readByte() {
        var value = array[offset + position];
        position++;
        return value;
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) {
        Objects.checkFromIndexSize(off, len, dst.length);
        System.arraycopy(array, offset + position, dst, off, len);
        position += len;
    }

    @Override
    public short readShort() {
        var value = ArrayUtils.getShort(array, offset + position, order);
        position += Short.BYTES;
        return value;
    }

    @Override
    public int readInt() {
        var value = ArrayUtils.getInt(array, offset + position, order);
        position += Integer.BYTES;
        return value;
    }

    @Override
    public long readLong() {
        var value = ArrayUtils.getLong(array, offset + position, order);
        position += Long.BYTES;
        return value;
    }

    @Override
    public float readFloat() {
        var value = ArrayUtils.getFloat(array, offset + position, order);
        position += Float.BYTES;
        return value;
    }

    @Override
    public double readDouble() {
        var value = ArrayUtils.getDouble(array, offset + position, order);
        position += Double.BYTES;
        return value;
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public void position(long position) {
        int pos = Math.toIntExact(position);
        Objects.checkIndex(pos, length + 1);
        this.position = pos;
    }

    @Override
    public ByteOrder order() {
        return order;
    }

    @Override
    public void order(ByteOrder order) {
        this.order = order;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public String toString() {
        return "ByteArrayDataSource[position=" + position + ", size=" + length + "]";
    }
}
