package com.shade.decima;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import static java.nio.file.StandardOpenOption.*;

public class DataStorageArchive implements Closeable {
    private final LZ4SafeDecompressor decompressor;
    private final SeekableByteChannel channel;

    private final Header header;
    private final NavigableMap<Long, Chunk> chunks;

    public static void main(String[] args) throws Exception {
        final Path path = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition/LocalCacheWinGame/package/package.00.11.core");
        final Path output = path.resolveSibling(path.getFileName() + ".unpacked");

        try (
            DataStorageArchive archive = new DataStorageArchive(path);
            WritableByteChannel channel = Files.newByteChannel(output, WRITE, CREATE, TRUNCATE_EXISTING)
        ) {
            archive.extract(channel);
        }
    }

    public DataStorageArchive(@NotNull Path path) throws IOException {
        this.decompressor = LZ4Factory.safeInstance().safeDecompressor();
        this.channel = Files.newByteChannel(path, StandardOpenOption.READ);
        this.header = Header.read(BufferUtils.readFromChannel(channel, Header.BYTES));
        this.chunks = new TreeMap<>(Long::compareUnsigned);

        final ByteBuffer buffer = BufferUtils.readFromChannel(channel, Chunk.BYTES * header.chunkCount());
        for (int i = 0; i < header.chunkCount(); i++) {
            final Chunk chunk = Chunk.read(buffer);
            chunks.put(chunk.offset(), chunk);
        }
    }

    public void extract(@NotNull WritableByteChannel output) throws IOException {
        final ByteBuffer compressed = ByteBuffer.allocate(0x50000);
        final ByteBuffer decompressed = ByteBuffer.allocate(0x50000);

        for (Chunk chunk : chunks.values()) {
            channel.position(chunk.compressedOffset());
            channel.read(compressed.slice(0, chunk.compressedSize()));

            if (chunk.type == 3) {
                decompressor.decompress(compressed, 0, chunk.compressedSize(), decompressed, 0, chunk.size());
            } else {
                throw new IllegalArgumentException("Unsupported chunk type: " + chunk.type);
            }

            output.write(decompressed.slice(0, chunk.size()));
        }
    }

    @NotNull
    public InputStream newInputStream(long offset, int size) {
        return new ArchiveInputStream(offset, size);
    }

    @NotNull
    public NavigableMap<Long, Chunk> getChunks(long offset, int size) {
        final NavigableMap<Long, Chunk> map = chunks.subMap(
            chunks.floorKey(offset), true,
            chunks.floorKey(offset + size), true
        );

        if (map.isEmpty()) {
            throw new IllegalArgumentException(String.format("Can't find any chunk entries for span starting at %#x (size: %#x)", offset, size));
        }

        assert map.firstEntry().getValue().contains(offset);
        assert map.lastEntry().getValue().contains(offset + size);

        return map;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public record Header(
        int magic,
        short versionMajor,
        short versionMinor,
        int chunkCount,
        int firstChunkOffset,
        long totalSize
    ) {
        public static final int BYTES = 32;
        public static final int MAGIC = 'D' | 'S' << 8 | 'A' << 16 | 'R' << 24;

        @NotNull
        public static Header read(@NotNull ByteBuffer buffer) {
            final var magic = buffer.getInt();
            final var versionMajor = buffer.getShort();
            final var versionMinor = buffer.getShort();
            final var chunkCount = buffer.getInt();
            final var firstChunkOffset = buffer.getInt();
            final var totalSize = buffer.getLong();

            // Skip padding
            buffer.position(buffer.position() + 8);

            return new Header(magic, versionMajor, versionMinor, chunkCount, firstChunkOffset, totalSize);
        }
    }

    public record Chunk(
        long offset,
        long compressedOffset,
        int size,
        int compressedSize,
        byte type
    ) implements Comparable<Chunk> {
        public static final int BYTES = 32;

        @NotNull
        public static Chunk read(@NotNull ByteBuffer buffer) {
            final var offset = buffer.getLong();
            final var compressedOffset = buffer.getLong();
            final var size = buffer.getInt();
            final var compressedSize = buffer.getInt();
            final var type = buffer.get();

            // Skip padding
            buffer.position(buffer.position() + 7);

            return new Chunk(offset, compressedOffset, size, compressedSize, type);
        }

        public boolean contains(long offset) {
            return offset >= this.offset && offset <= this.offset + size;
        }

        @Override
        public int compareTo(Chunk o) {
            return Long.compareUnsigned(offset, o.offset);
        }
    }

    protected static abstract class ChunkedInputStream extends InputStream {
        protected final byte[] compressed;
        protected final byte[] decompressed;

        protected int count; // count of bytes in the current chunk
        protected int pos; // position in the current chunk

        public ChunkedInputStream(int compressedBufferSize, int decompressedBufferSie) {
            this.compressed = new byte[compressedBufferSize];
            this.decompressed = new byte[decompressedBufferSie];
        }

        @Override
        public int read() throws IOException {
            if (pos >= count) {
                fill();
            }
            if (pos >= count) {
                return -1;
            }
            return decompressed[pos++] & 0xff;
        }

        @Override
        public int read(@NotNull byte[] buf, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, buf.length);

            if (len == 0) {
                return 0;
            }

            for (int n = 0; ; ) {
                final int read = read1(buf, off + n, len - n);
                if (read <= 0) {
                    return n == 0 ? read : n;
                }
                n += read;
                if (n >= len) {
                    return n;
                }
            }
        }

        private int read1(@NotNull byte[] buf, int off, int len) throws IOException {
            int available = count - pos;

            if (available <= 0) {
                fill();
                available = count - pos;
            }

            if (available <= 0) {
                return -1;
            }

            final int count = Math.min(available, len);
            System.arraycopy(decompressed, pos, buf, off, count);
            pos += count;

            return count;
        }

        protected abstract void fill() throws IOException;
    }

    private class ArchiveInputStream extends ChunkedInputStream {
        private final Chunk[] chunks;
        private final long offset;
        private final long size;

        private int index; // index of the current chunk

        public ArchiveInputStream(long offset, int size) {
            super(0x50000, 0x50000);
            this.chunks = getChunks(offset, size).values().toArray(Chunk[]::new);
            this.offset = offset;
            this.size = size;
        }

        @Override
        protected void fill() throws IOException {
            if (index >= chunks.length) {
                return;
            }

            final Chunk chunk = chunks[index];
            final ByteBuffer buffer = ByteBuffer.wrap(compressed, 0, chunk.compressedSize);

            synchronized (DataStorageArchive.this) {
                channel.position(chunk.compressedOffset);
                channel.read(buffer.slice());
            }

            if (chunk.type == 3) {
                decompressor.decompress(compressed, 0, chunk.compressedSize(), decompressed, 0, chunk.size());
            } else {
                throw new IllegalArgumentException("Unsupported chunk compression type: " + chunk.type);
            }

            if (index == 0) {
                pos = (int) (offset - chunk.offset);
            } else {
                pos = 0;
            }

            if (index == chunks.length - 1) {
                count = (int) (offset + size - chunk.offset);
            } else {
                count = chunk.size;
            }

            index++;
        }
    }
}
