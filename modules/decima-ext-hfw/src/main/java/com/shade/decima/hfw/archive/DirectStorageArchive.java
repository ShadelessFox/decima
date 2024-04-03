package com.shade.decima.hfw.archive;

import com.shade.decima.model.archive.ChunkedInputStream;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.nio.file.StandardOpenOption.*;

public class DirectStorageArchive implements Closeable {
    private final LZ4SafeDecompressor decompressor;
    private final SeekableByteChannel channel;

    private final Header header;
    private final NavigableMap<Long, Chunk> chunks;

    public static void main(String[] args) throws Exception {
        final Path path = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition/LocalCacheWinGame/package/package.00.11.core");
        final Path output = path.resolveSibling(path.getFileName() + ".unpacked");

        try (
            DirectStorageArchive archive = new DirectStorageArchive(path);
            InputStream is = archive.newInputStream(0, archive.header.totalSize);
            OutputStream os = Files.newOutputStream(output, CREATE, WRITE, TRUNCATE_EXISTING)
        ) {
            is.transferTo(os);
        }
    }

    public DirectStorageArchive(@NotNull Path path) throws IOException {
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

    @NotNull
    public InputStream newInputStream(long offset, long size) {
        return new ArchiveInputStream(offset, size);
    }

    @NotNull
    public NavigableMap<Long, Chunk> getChunks(long offset, long size) {
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
        public static Header read(@NotNull ByteBuffer buffer) throws IOException {
            final var magic = buffer.getInt();
            final var versionMajor = buffer.getShort();
            final var versionMinor = buffer.getShort();
            final var chunkCount = buffer.getInt();
            final var firstChunkOffset = buffer.getInt();
            final var totalSize = buffer.getLong();

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

    public record Chunk(
        long offset,
        long compressedOffset,
        int size,
        int compressedSize,
        byte type
    ) implements Comparable<Chunk> {
        public static final int BYTES = 32;
        public static final int TYPE_LZ4 = 3;

        @NotNull
        public static Chunk read(@NotNull ByteBuffer buffer) {
            final var offset = buffer.getLong();
            final var compressedOffset = buffer.getLong();
            final var size = buffer.getInt();
            final var compressedSize = buffer.getInt();
            final var type = buffer.get();

            // Skip padding
            buffer.position(buffer.position() + 7);

            if (type != TYPE_LZ4) {
                throw new IllegalArgumentException("Unsupported chunk compression type: " + type);
            }

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

    private class ArchiveInputStream extends ChunkedInputStream {
        private final Chunk[] chunks;
        private final long offset;
        private final long size;

        private int index; // index of the current chunk

        public ArchiveInputStream(long offset, long size) {
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

            synchronized (DirectStorageArchive.this) {
                channel.position(chunk.compressedOffset);
                channel.read(buffer.slice());
            }

            decompressor.decompress(compressed, 0, chunk.compressedSize(), decompressed, 0, chunk.size());

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
