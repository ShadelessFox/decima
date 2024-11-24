package com.shade.util.io;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;

final class ByteArrayBinaryReader implements BinaryReader {
    private static final VarHandle asShortVarHandle = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asIntVarHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asLongVarHandle = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asFloatVarHandle = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asDoubleVarHandle = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

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
        var value = (short) asShortVarHandle.get(array, offset + position);
        position += Short.BYTES;
        return value;
    }

    @Override
    public int readInt() {
        var value = (int) asIntVarHandle.get(array, offset + position);
        position += Integer.BYTES;
        return value;
    }

    @Override
    public long readLong() {
        var value = (long) asLongVarHandle.get(array, offset + position);
        position += Long.BYTES;
        return value;
    }

    @Override
    public float readFloat() {
        var value = (float) asFloatVarHandle.get(array, offset + position);
        position += Float.BYTES;
        return value;
    }

    @Override
    public double readDouble() {
        var value = (double) asDoubleVarHandle.get(array, offset + position);
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
