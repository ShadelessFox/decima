package com.shade.util.io;

import com.shade.util.ThrowableFunction;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * A generic source of data.
 * <p>
 * By default, underlying data is interpreted as little endian. Byte order can
 * be controlled using {@link #order(ByteOrder)} method.
 */
public interface BinaryReader extends Closeable {
    static BinaryReader wrap(ByteBuffer buffer) {
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("Buffer must be backed by an array");
        }
        return new ByteArrayBinaryReader(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
    }

    static BinaryReader wrap(byte[] array) {
        return new ByteArrayBinaryReader(array, 0, array.length);
    }

    static BinaryReader wrap(byte[] array, int off, int len) {
        return new ByteArrayBinaryReader(array, off, len);
    }

    static BinaryReader open(Path path) throws IOException {
        return new ChannelBinaryReader(Files.newByteChannel(path, StandardOpenOption.READ));
    }

    byte readByte() throws IOException;

    void readBytes(byte[] dst, int off, int len) throws IOException;

    default byte[] readBytes(int count) throws IOException {
        var dst = new byte[count];
        readBytes(dst, 0, count);
        return dst;
    }

    short readShort() throws IOException;

    default void readShorts(short[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readShort();
        }
    }

    default short[] readShorts(int count) throws IOException {
        var dst = new short[count];
        readShorts(dst, 0, count);
        return dst;
    }

    int readInt() throws IOException;

    default void readInts(int[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readInt();
        }
    }

    default int[] readInts(int count) throws IOException {
        var dst = new int[count];
        readInts(dst, 0, count);
        return dst;
    }

    long readLong() throws IOException;

    default void readLongs(long[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readLong();
        }
    }

    default long[] readLongs(int count) throws IOException {
        var dst = new long[count];
        readLongs(dst, 0, count);
        return dst;
    }

    float readFloat() throws IOException;

    default void readFloats(float[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readFloat();
        }
    }

    default float[] readFloats(int count) throws IOException {
        var dst = new float[count];
        readFloats(dst, 0, count);
        return dst;
    }

    double readDouble() throws IOException;

    default void readDoubles(double[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        for (int i = 0; i < len; i++) {
            dst[off + i] = readDouble();
        }
    }

    default double[] readDoubles(int count) throws IOException {
        var dst = new double[count];
        readDoubles(dst, 0, count);
        return dst;
    }

    default String readString(int length, Charset charset) throws IOException {
        return new String(readBytes(length), charset);
    }

    default String readString(int length) throws IOException {
        return readString(length, StandardCharsets.UTF_8);
    }

    default boolean readByteBoolean() throws IOException {
        var value = readByte();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IOException("Invalid boolean value: " + value);
        };
    }

    default boolean readIntBoolean() throws IOException {
        var value = readInt();
        return switch (value) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IOException("Invalid boolean value: " + value);
        };
    }

    default <T> T[] readObjects(int count, ThrowableFunction<BinaryReader, T, IOException> mapper, IntFunction<T[]> creator) throws IOException {
        var dst = creator.apply(count);
        for (int i = 0; i < count; i++) {
            dst[i] = mapper.apply(this);
        }
        return dst;
    }

    default <T> List<T> readObjects(int count, ThrowableFunction<BinaryReader, T, IOException> mapper) throws IOException {
        var dst = new ArrayList<T>(count);
        for (int i = 0; i < count; i++) {
            dst.add(mapper.apply(this));
        }
        return List.copyOf(dst);
    }

    long size() throws IOException;

    long position() throws IOException;

    void position(long pos) throws IOException;

    ByteOrder order();

    void order(ByteOrder order);

    default long remaining() throws IOException {
        return size() - position();
    }

    default void skip(int count) throws IOException {
        Objects.checkIndex(count, Integer.MAX_VALUE);
        position(position() + count);
    }
}
