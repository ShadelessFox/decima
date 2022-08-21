package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PackfileWriter implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileWriter.class);

    private final SortedSet<Resource> resources;

    public PackfileWriter() {
        this.resources = new TreeSet<>(Comparator.comparingLong(Resource::hash));
    }

    public boolean add(@NotNull Resource resource) {
        return resources.add(resource);
    }

    public void write(@NotNull FileChannel channel, @NotNull Compressor compressor, @NotNull Compressor.Level level, @NotNull ProgressMonitor monitor, boolean encrypt) throws IOException {
        final Set<PackfileBase.FileEntry> files = new TreeSet<>();
        final Set<PackfileBase.ChunkEntry> chunks = new TreeSet<>();

        channel.position(computeHeaderSize());
        writeData(channel, files, chunks, compressor, level, encrypt);

        channel.position(0);
        writeHeader(channel, files, chunks, encrypt);
    }

    private void writeHeader(@NotNull FileChannel channel, @NotNull Set<PackfileBase.FileEntry> files, @NotNull Set<PackfileBase.ChunkEntry> chunks, boolean encrypt) throws IOException {
        final long decompressedSize = chunks.stream()
            .mapToLong(entry -> entry.decompressed().size())
            .sum();

        final long compressedSize = chunks.stream()
            .mapToLong(entry -> entry.compressed().size())
            .sum();

        final int headerSize = computeHeaderSize();

        final ByteBuffer buffer = ByteBuffer
            .allocate(headerSize)
            .order(ByteOrder.LITTLE_ENDIAN);

        final PackfileBase.Header header = new PackfileBase.Header(
            encrypt ? PackfileBase.MAGIC_ENCRYPTED : PackfileBase.MAGIC_PLAIN,
            encrypt ? ThreadLocalRandom.current().nextInt() : 0,
            compressedSize + headerSize,
            decompressedSize,
            files.size(),
            chunks.size(),
            Compressor.BLOCK_SIZE_BYTES
        );

        header.write(buffer);

        for (PackfileBase.FileEntry file : files) {
            file.write(buffer, encrypt);
        }

        for (PackfileBase.ChunkEntry chunk : chunks) {
            chunk.write(buffer, encrypt);
        }

        channel.write(buffer.position(0));
    }

    private void writeData(@NotNull FileChannel channel, @NotNull Set<PackfileBase.FileEntry> files, @NotNull Set<PackfileBase.ChunkEntry> chunks, @NotNull Compressor compressor, @NotNull Compressor.Level level, boolean encrypt) throws IOException {
        final Queue<Resource> pending = new ArrayDeque<>(resources);
        final ByteBuffer decompressed = ByteBuffer.allocate(Compressor.BLOCK_SIZE_BYTES);

        long fileDataOffset = 0;
        long chunkDataDecompressedOffset = 0;
        long chunkDataCompressedOffset = channel.position();

        while (!pending.isEmpty()) {
            decompressed.clear();

            while (decompressed.hasRemaining() && !pending.isEmpty()) {
                final Resource resource = pending.element();
                final long length = resource.read(decompressed);

                if (length <= 0) {
                    pending.remove();

                    files.add(new PackfileBase.FileEntry(
                        files.size(),
                        encrypt ? ThreadLocalRandom.current().nextInt() : 0,
                        resource.hash(),
                        new PackfileBase.Span(
                            fileDataOffset,
                            resource.size(),
                            encrypt ? ThreadLocalRandom.current().nextInt() : 0
                        )
                    ));

                    fileDataOffset += resource.size();

                    if (log.isDebugEnabled()) {
                        log.debug("[%d/%d] File '%s' was written (size: %s)".formatted(files.size(), resources.size(), resource.hash(), IOUtils.formatSize(resource.size())));
                    }
                }
            }

            decompressed.limit(decompressed.position());
            decompressed.position(0);

            final ByteBuffer compressed = compressor.compress(decompressed.slice(), level);

            final PackfileBase.Span decompressedSpan = new PackfileBase.Span(
                chunkDataDecompressedOffset,
                decompressed.remaining(),
                encrypt ? ThreadLocalRandom.current().nextInt() : 0
            );

            final PackfileBase.Span compressedSpan = new PackfileBase.Span(
                chunkDataCompressedOffset,
                compressed.remaining(),
                encrypt ? ThreadLocalRandom.current().nextInt() : 0
            );

            if (encrypt) {
                PackfileBase.ChunkEntry.swizzle(compressed, decompressedSpan);
            }

            chunks.add(new PackfileBase.ChunkEntry(decompressedSpan, compressedSpan));
            chunkDataDecompressedOffset += decompressed.remaining();
            chunkDataCompressedOffset += compressed.remaining();

            channel.write(compressed);
        }
    }

    @Override
    public void close() throws IOException {
        for (Resource resource : resources) {
            resource.close();
        }

        resources.clear();
    }

    private int computeHeaderSize() {
        return PackfileBase.Header.BYTES
               + PackfileBase.FileEntry.BYTES * resources.size()
               + PackfileBase.ChunkEntry.BYTES * computeChunksCount();
    }

    private int computeChunksCount() {
        final long size = resources.stream()
            .mapToLong(Resource::size)
            .sum();

        return Compressor.getBlocksCount(size);
    }
}
