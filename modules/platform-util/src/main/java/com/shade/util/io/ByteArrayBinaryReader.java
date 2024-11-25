package com.shade.util.io;

import com.shade.util.ArrayUtils;

import java.util.Objects;

final class ByteArrayBinaryReader implements BinaryReader {
    private final byte[] array;
    private final int offset;
    private final int length;
    private int position;

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
        var value = ArrayUtils.getShort(array, offset + position);
        position += Short.BYTES;
        return value;
    }

    @Override
    public int readInt() {
        var value = ArrayUtils.getInt(array, offset + position);
        position += Integer.BYTES;
        return value;
    }

    @Override
    public long readLong() {
        var value = ArrayUtils.getLong(array, offset + position);
        position += Long.BYTES;
        return value;
    }

    @Override
    public float readFloat() {
        var value = ArrayUtils.getFloat(array, offset + position);
        position += Float.BYTES;
        return value;
    }

    @Override
    public double readDouble() {
        var value = ArrayUtils.getDouble(array, offset + position);
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
        Objects.checkIndex(pos, length);
        this.position = pos;
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
