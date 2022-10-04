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
import java.nio.channels.SeekableByteChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;

public class PackfileWriter implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PackfileWriter.class);

    private final SortedSet<Resource> resources;

    public PackfileWriter() {
        this.resources = new TreeSet<>(Comparator.comparingLong(Resource::hash));
    }

    public boolean add(@NotNull Resource resource) {
        return resources.add(resource);
    }

    public long write(
        @NotNull ProgressMonitor monitor,
        @NotNull SeekableByteChannel channel,
        @NotNull Compressor compressor,
        @NotNull Options options
    ) throws IOException {
        final RandomGenerator random = new SecureRandom();
        final Set<PackfileBase.FileEntry> files = new TreeSet<>();
        final Set<PackfileBase.ChunkEntry> chunks = new TreeSet<>();

        channel.position(computeHeaderSize());
        writeData(monitor, channel, compressor, random, options, files, chunks);

        channel.position(0);
        return writeHeader(monitor, channel, random, options, files, chunks).fileSize();
    }

    @NotNull
    private PackfileBase.Header writeHeader(
        @NotNull ProgressMonitor monitor,
        @NotNull SeekableByteChannel channel,
        @NotNull RandomGenerator random,
        @NotNull Options options,
        @NotNull Set<PackfileBase.FileEntry> files,
        @NotNull Set<PackfileBase.ChunkEntry> chunks
    ) throws IOException {
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
            options.encrypt() ? PackfileBase.MAGIC_ENCRYPTED : PackfileBase.MAGIC_PLAIN,
            options.encrypt() ? random.nextInt() : 0,
            compressedSize + headerSize,
            decompressedSize,
            files.size(),
            chunks.size(),
            Compressor.BLOCK_SIZE_BYTES
        );

        header.write(buffer);

        for (PackfileBase.FileEntry file : files) {
            file.write(buffer, options.encrypt());
        }

        for (PackfileBase.ChunkEntry chunk : chunks) {
            chunk.write(buffer, options.encrypt());
        }

        channel.write(buffer.position(0));

        return header;
    }

    private void writeData(
        @NotNull ProgressMonitor monitor,
        @NotNull SeekableByteChannel channel,
        @NotNull Compressor compressor,
        @NotNull RandomGenerator random,
        @NotNull Options options,
        @NotNull Set<PackfileBase.FileEntry> files,
        @NotNull Set<PackfileBase.ChunkEntry> chunks
    ) throws IOException {
        final Queue<Resource> pending = new ArrayDeque<>(resources);
        final ByteBuffer decompressed = ByteBuffer.allocate(Compressor.BLOCK_SIZE_BYTES);

        long fileDataOffset = 0;
        long chunkDataDecompressedOffset = 0;
        long chunkDataCompressedOffset = channel.position();

        while (!pending.isEmpty()) {
            boolean skip = true;

            decompressed.clear();

            while (decompressed.hasRemaining() && !pending.isEmpty()) {
                final Resource resource = pending.element();
                final long length = resource.read(decompressed);

                if (length <= 0) {
                    pending.remove().close();

                    files.add(new PackfileBase.FileEntry(
                        files.size(),
                        options.encrypt() ? random.nextInt() : 0,
                        resource.hash(),
                        new PackfileBase.Span(
                            fileDataOffset,
                            resource.size(),
                            options.encrypt() ? random.nextInt() : 0
                        )
                    ));

                    fileDataOffset += resource.size();
                    skip &= resource.size() > 0;

                    if (log.isDebugEnabled()) {
                        log.debug("[%d/%d] File '%s' was written (size: %s)".formatted(files.size(), resources.size(), resource.hash(), IOUtils.formatSize(resource.size())));
                    }
                } else {
                    skip = false;
                }
            }

            if (skip) {
                continue;
            }

            decompressed.limit(decompressed.position());
            decompressed.position(0);

            final ByteBuffer compressed = compressor.compress(decompressed.slice(), options.compression());

            final PackfileBase.Span decompressedSpan = new PackfileBase.Span(
                chunkDataDecompressedOffset,
                decompressed.remaining(),
                options.encrypt() ? random.nextInt() : 0
            );

            final PackfileBase.Span compressedSpan = new PackfileBase.Span(
                chunkDataCompressedOffset,
                compressed.remaining(),
                options.encrypt() ? random.nextInt() : 0
            );

            if (options.encrypt()) {
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

        return Math.max(1, Compressor.getBlocksCount(size));
    }

    public record Options(@NotNull Compressor.Level compression, boolean encrypt) {}
}
