package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;

public class PackfileWriter implements Closeable {
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
        final Set<Packfile.FileEntry> files = new TreeSet<>();
        final Set<Packfile.ChunkEntry> chunks = new TreeSet<>();

        try (ProgressMonitor.Task task = monitor.begin("Write packfile", 2)) {
            channel.position(computeHeaderSize());
            writeData(task.split(1), channel, compressor, random, options, files, chunks);

            channel.position(0);
            return writeHeader(task.split(1), channel, random, options, files, chunks).fileSize();
        }
    }

    @NotNull
    private Packfile.Header writeHeader(
        @NotNull ProgressMonitor monitor,
        @NotNull SeekableByteChannel channel,
        @NotNull RandomGenerator random,
        @NotNull Options options,
        @NotNull Set<Packfile.FileEntry> files,
        @NotNull Set<Packfile.ChunkEntry> chunks
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

        final Packfile.Header header = new Packfile.Header(
            options.encrypt() ? Packfile.MAGIC_ENCRYPTED : Packfile.MAGIC_PLAIN,
            options.encrypt() ? random.nextInt() : 0,
            compressedSize + headerSize,
            decompressedSize,
            files.size(),
            chunks.size(),
            Packfile.MAXIMUM_BLOCK_SIZE
        );

        try (ProgressMonitor.Task task = monitor.begin("Write header", 1)) {
            header.write(buffer);

            for (Packfile.FileEntry file : files) {
                file.write(buffer, options.encrypt());
            }

            for (Packfile.ChunkEntry chunk : chunks) {
                chunk.write(buffer, options.encrypt());
            }

            channel.write(buffer.position(0));
            task.worked(1);
        }

        return header;
    }

    private void writeData(
        @NotNull ProgressMonitor monitor,
        @NotNull SeekableByteChannel channel,
        @NotNull Compressor compressor,
        @NotNull RandomGenerator random,
        @NotNull Options options,
        @NotNull Set<Packfile.FileEntry> files,
        @NotNull Set<Packfile.ChunkEntry> chunks
    ) throws IOException {
        final Queue<Resource> pending = new ArrayDeque<>(resources);
        final ByteBuffer decompressed = ByteBuffer.allocate(Packfile.MAXIMUM_BLOCK_SIZE);

        long fileDataOffset = 0;
        long chunkDataDecompressedOffset = 0;
        long chunkDataCompressedOffset = channel.position();

        try (ProgressMonitor.Task task = monitor.begin("Write files", pending.size())) {
            while (!pending.isEmpty() && !task.isCanceled()) {
                boolean skip = true;

                decompressed.clear();

                while (decompressed.hasRemaining() && !pending.isEmpty()) {
                    final Resource resource = pending.element();
                    final int length = resource.read(decompressed);

                    if (length <= 0) {
                        pending.remove().close();

                        files.add(new Packfile.FileEntry(
                            files.size(),
                            options.encrypt() ? random.nextInt() : 0,
                            resource.hash(),
                            new Packfile.Span(
                                fileDataOffset,
                                resource.size(),
                                options.encrypt() ? random.nextInt() : 0
                            )
                        ));

                        fileDataOffset += resource.size();
                        skip &= resource.size() > 0;

                        task.worked(1);
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

                final Packfile.Span decompressedSpan = new Packfile.Span(
                    chunkDataDecompressedOffset,
                    decompressed.remaining(),
                    options.encrypt() ? random.nextInt() : 0
                );

                final Packfile.Span compressedSpan = new Packfile.Span(
                    chunkDataCompressedOffset,
                    compressed.remaining(),
                    options.encrypt() ? random.nextInt() : 0
                );

                if (options.encrypt()) {
                    Packfile.ChunkEntry.swizzle(compressed, decompressedSpan);
                }

                chunks.add(new Packfile.ChunkEntry(decompressedSpan, compressedSpan));
                chunkDataDecompressedOffset += decompressed.remaining();
                chunkDataCompressedOffset += compressed.remaining();

                channel.write(compressed);
            }
        }
    }

    @NotNull
    public Collection<Resource> getResources() {
        return Collections.unmodifiableCollection(resources);
    }

    @Override
    public void close() throws IOException {
        for (Resource resource : resources) {
            resource.close();
        }

        resources.clear();
    }

    private int computeHeaderSize() {
        return Packfile.Header.BYTES
            + Packfile.FileEntry.BYTES * resources.size()
            + Packfile.ChunkEntry.BYTES * computeChunksCount();
    }

    private int computeChunksCount() {
        final long size = resources.stream()
            .mapToLong(Resource::size)
            .sum();

        return Math.max(1, Math.toIntExact(Math.ceilDiv(size, Packfile.MAXIMUM_BLOCK_SIZE)));
    }

    public record Options(@NotNull Compressor.Level compression, boolean encrypt) {}
}
