package com.shade.decima.model.archive;

import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ArchiveWriter implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ArchiveWriter.class);

    private static final int HEADER_SIZE_BYTES = 40;
    private static final int FILE_ENTRY_SIZE_BYTES = 32;
    private static final int CHUNK_ENTRY_SIZE_BYTES = 32;

    private final FileChannel channel;
    private final Compressor compressor;
    private final List<ArchiveResource> input;

    public ArchiveWriter(@NotNull Path path, @NotNull Compressor compressor) throws IOException {
        this.channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        this.compressor = compressor;
        this.input = new ArrayList<>();
    }

    public void append(@NotNull ArchiveResource resource) {
        input.add(resource);
    }

    @Override
    public void close() throws IOException {
        final List<FileEntry> files = new ArrayList<>();
        final List<ChunkEntry> chunks = new ArrayList<>();

        channel.position(getHeaderSize());

        writeFiles(files, chunks);

        channel.position(0);

        writeHeader(files, chunks);

        channel.close();
    }

    private void writeFiles(@NotNull List<FileEntry> files, @NotNull List<ChunkEntry> chunks) throws IOException {
        final Deque<ArchiveResource> queue = new ArrayDeque<>(input);
        final ByteBuffer acc = ByteBuffer.allocate(Compressor.BLOCK_SIZE_BYTES);

        long fileDataOffset = 0;
        long chunkDataOriginalOffset = 0;
        long chunkDataCompressedOffset = channel.position();

        while (!queue.isEmpty()) {
            acc.clear();

            while (acc.hasRemaining() && !queue.isEmpty()) {
                final ArchiveResource resource = queue.element();
                final int length = resource.read(acc);

                if (length <= 0) {
                    files.add(new FileEntry(resource, new EntrySpan(fileDataOffset, resource.size())));
                    fileDataOffset += resource.size();
                    queue.remove();

                    if (log.isDebugEnabled()) {
                        log.debug("[%d/%d] File '%s' was written (size: %s)".formatted(files.size(), input.size(), resource.path(), IOUtils.formatSize(resource.size())));
                    }
                }
            }

            acc.limit(acc.position());
            acc.position(0);

            final byte[] src = IOUtils.getBytesExact(acc, acc.remaining());
            final byte[] dst = new byte[Compressor.getCompressedSize(src.length)];
            final int length = compressor.compress(src, dst);

            channel.write(ByteBuffer.wrap(dst, 0, length));

            chunks.add(new ChunkEntry(new EntrySpan(chunkDataOriginalOffset, src.length), new EntrySpan(chunkDataCompressedOffset, length)));
            chunkDataOriginalOffset += src.length;
            chunkDataCompressedOffset += length;
        }
    }

    private void writeHeader(@NotNull List<FileEntry> files, @NotNull List<ChunkEntry> chunks) throws IOException {
        final long originalSize = chunks.stream()
            .mapToLong(entry -> entry.original().length())
            .sum();

        final long compressedSize = chunks.stream()
            .mapToLong(entry -> entry.compressed().length())
            .sum();

        final int headerSize = getHeaderSize();

        final ByteBuffer buffer = ByteBuffer
            .allocate(headerSize)
            .order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(0x20304050);
        buffer.putInt(0);
        buffer.putLong(compressedSize + headerSize);
        buffer.putLong(originalSize);
        buffer.putLong(files.size());
        buffer.putInt(chunks.size());
        buffer.putInt(Compressor.BLOCK_SIZE_BYTES);

        for (int i = 0; i < files.size(); i++) {
            final FileEntry file = files.get(i);

            buffer.putInt(i);
            buffer.putInt(0);
            buffer.putLong(file.resource().hash());

            buffer.putLong(file.span().offset());
            buffer.putInt(file.span().length());
            buffer.putInt(0);
        }

        for (ChunkEntry chunk : chunks) {
            buffer.putLong(chunk.original().offset());
            buffer.putInt(chunk.original().length());
            buffer.putInt(0);

            buffer.putLong(chunk.compressed().offset());
            buffer.putInt(chunk.compressed().length());
            buffer.putInt(0);
        }

        channel.write(buffer.position(0));

        if (log.isDebugEnabled()) {
            log.debug("Files in total: %d (original size: %s, compressed size: %s (%+.02f%%)".formatted(files.size(), IOUtils.formatSize(originalSize), IOUtils.formatSize(compressedSize), (compressedSize - originalSize) / Math.abs((double) originalSize) * 100));
        }
    }

    private int getHeaderSize() throws IOException {
        return HEADER_SIZE_BYTES
            + FILE_ENTRY_SIZE_BYTES * input.size()
            + CHUNK_ENTRY_SIZE_BYTES * getChunksCount();
    }

    private int getChunksCount() throws IOException {
        long size = 0;

        for (ArchiveResource resource : input) {
            size += resource.size();
        }

        return Compressor.getBlocksCount(size);
    }

    private static record FileEntry(@NotNull ArchiveResource resource, @NotNull EntrySpan span) {
    }

    private static record ChunkEntry(@NotNull EntrySpan original, @NotNull EntrySpan compressed) {
    }

    private static record EntrySpan(long offset, int length) {
    }
}
