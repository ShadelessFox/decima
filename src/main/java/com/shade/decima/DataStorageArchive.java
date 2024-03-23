package com.shade.decima;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.nio.file.StandardOpenOption.*;

public class DataStorageArchive implements Closeable {
    private final LZ4SafeDecompressor decompressor;
    private final SeekableByteChannel channel;

    private final Header header;
    private final NavigableMap<Long, Chunk> chunks;

    public static void main(String[] args) throws Exception {
        final Path path = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition/LocalCacheWinGame/package/package.00.00.core");
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

        @Override
        public int compareTo(Chunk o) {
            return Long.compareUnsigned(offset, o.offset);
        }
    }
}
