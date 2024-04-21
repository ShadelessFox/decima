package com.shade.decima.hfw.archive;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StorageChannel implements SeekableByteChannel {
    private static final LZ4FastDecompressor decompressor = LZ4Factory.fastestJavaInstance().fastDecompressor();

    private final SeekableByteChannel channel;
    private final Header header;
    private final NavigableMap<Long, Chunk> chunks;

    private long position;
    private Chunk chunk;
    private byte[] buffer;

    private StorageChannel(@NotNull SeekableByteChannel channel) throws IOException {
        this.channel = channel;
        this.header = Header.read(BufferUtils.readFromChannel(channel, Header.BYTES));
        this.chunks = new TreeMap<>(Long::compareUnsigned);

        final ByteBuffer buffer = BufferUtils.readFromChannel(channel, Chunk.BYTES * header.chunkCount());
        for (int i = 0; i < header.chunkCount(); i++) {
            final Chunk chunk = Chunk.read(buffer);
            chunks.put(chunk.offset(), chunk);
        }
    }

    @NotNull
    public static SeekableByteChannel newChannel(@NotNull Path path) throws IOException {
        final SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
        final ByteBuffer buffer = BufferUtils.readFromChannel(channel, 4);

        channel.position(0);

        if (buffer.getInt() == Header.MAGIC) {
            return new StorageChannel(channel);
        } else {
            return channel;
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (position >= size()) {
            return -1;
        }

        long start = position;

        while (dst.hasRemaining()) {
            final Chunk chunk = chunks.floorEntry(position).getValue();
            final int offset = Math.toIntExact(position - chunk.offset);
            final int length = Math.min(chunk.size - offset, dst.remaining());

            if (this.chunk != chunk) {
                this.chunk = chunk;
                this.buffer = new byte[chunk.size];

                final ByteBuffer compressed = ByteBuffer.allocate(chunk.compressedSize);

                if (channel instanceof FileChannel fc) {
                    fc.read(compressed, chunk.compressedOffset);
                } else {
                    synchronized (channel) {
                        channel.position(chunk.compressedOffset).read(compressed);
                    }
                }

                decompressor.decompress(compressed.array(), 0, buffer, 0, chunk.size);
            }

            dst.put(buffer, offset, length);
            position += length;
        }

        return Math.toIntExact(position - start);
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) {
        if (newPosition < 0) {
            throw new IllegalArgumentException("Invalid position: " + newPosition);
        }
        position = newPosition;
        return this;
    }

    @Override
    public long size() {
        return header.totalSize;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        channel.close();
        buffer = null;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    private record Header(
        int magic,
        short versionMajor,
        short versionMinor,
        int chunkCount,
        int firstChunkOffset,
        long totalSize
    ) {
        static final int BYTES = 32;
        static final int MAGIC = 'D' | 'S' << 8 | 'A' << 16 | 'R' << 24;

        @NotNull
        static Header read(@NotNull ByteBuffer buffer) throws IOException {
            var magic = buffer.getInt();
            var versionMajor = buffer.getShort();
            var versionMinor = buffer.getShort();
            var chunkCount = buffer.getInt();
            var firstChunkOffset = buffer.getInt();
            var totalSize = buffer.getLong();

            // Skip padding
            buffer.position(buffer.position() + 8);

            if (magic != MAGIC) {
                throw new IOException("Invalid archive magic: %08x".formatted(magic));
            }

            if (versionMajor != 3 && versionMinor != 1) {
                throw new IOException("Unsupported archive version: %d.%d".formatted(versionMajor, versionMinor));
            }

            return new Header(magic, versionMajor, versionMinor, chunkCount, firstChunkOffset, totalSize);
        }
    }

    private record Chunk(
        long offset,
        long compressedOffset,
        int size,
        int compressedSize,
        byte type
    ) implements Comparable<Chunk> {
        static final int BYTES = 32;
        static final int TYPE_LZ4 = 3;

        @NotNull
        static Chunk read(@NotNull ByteBuffer buffer) {
            var offset = buffer.getLong();
            var compressedOffset = buffer.getLong();
            var size = buffer.getInt();
            var compressedSize = buffer.getInt();
            var type = buffer.get();

            // Skip padding
            buffer.position(buffer.position() + 7);

            if (type != TYPE_LZ4) {
                throw new IllegalArgumentException("Unsupported chunk compression type: " + type);
            }

            return new Chunk(offset, compressedOffset, size, compressedSize, type);
        }

        @Override
        public int compareTo(Chunk o) {
            return Long.compareUnsigned(offset, o.offset);
        }
    }
}
