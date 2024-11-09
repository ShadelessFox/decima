package com.shade.util.io;

import com.shade.util.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A reader for compressed data split into chunks.
 */
public abstract class ChunkedBinaryReader implements BinaryReader {
    public record Chunk(long offset, long compressedOffset, int size, int compressedSize) {}

    private static final VarHandle asShortVarHandle = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asIntVarHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle asLongVarHandle = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final BinaryReader reader;
    private final NavigableMap<Long, Chunk> chunks = new TreeMap<>(Long::compareUnsigned);

    private final byte[] compressed;
    private final byte[] decompressed;
    private final byte[] scratch = new byte[8];

    private Chunk chunk;
    private long position;

    protected ChunkedBinaryReader(@NotNull BinaryReader reader, @NotNull List<Chunk> chunks) {
        int maxCompressedChunkSize = 0;
        int maxDecompressedChunkSize = 0;

        for (Chunk chunk : chunks) {
            this.chunks.put(chunk.offset(), chunk);
            maxCompressedChunkSize = Math.max(maxCompressedChunkSize, chunk.compressedSize());
            maxDecompressedChunkSize = Math.max(maxDecompressedChunkSize, chunk.size());
        }

        this.reader = reader;
        this.compressed = new byte[maxCompressedChunkSize];
        this.decompressed = new byte[maxDecompressedChunkSize];
    }

    @Override
    public byte readByte() throws IOException {
        readBytes(scratch, 0, Byte.BYTES);
        return scratch[0];
    }

    @Override
    public short readShort() throws IOException {
        readBytes(scratch, 0, Short.BYTES);
        return (short) asShortVarHandle.get(scratch, 0);
    }

    @Override
    public int readInt() throws IOException {
        readBytes(scratch, 0, Integer.BYTES);
        return (int) asIntVarHandle.get(scratch, 0);
    }

    @Override
    public long readLong() throws IOException {
        readBytes(scratch, 0, Long.BYTES);
        return (long) asLongVarHandle.get(scratch, 0);
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) throws IOException {
        while (len > 0) {
            Chunk chunk = chunks.floorEntry(position).getValue();
            int offset = Math.toIntExact(position - chunk.offset());
            int length = Math.min(chunk.size() - offset, len);

            if (this.chunk != chunk) {
                this.chunk = chunk;

                reader.position(chunk.compressedOffset());
                reader.readBytes(compressed, 0, chunk.compressedSize());
                decompress(compressed, decompressed, chunk.size());
            }

            System.arraycopy(decompressed, offset, dst, off, length);
            position += length;
            off += length;
            len -= length;
        }
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public void position(long pos) throws IOException {
        Objects.checkIndex(pos, size());
        position = pos;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    protected abstract void decompress(@NotNull byte[] src, @NotNull byte[] dst, int length);
}
