package com.shade.decima.archive;

import com.shade.decima.util.Compressor;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;

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
    private static final int HEADER_SIZE_BYTES = 40;
    private static final int FILE_ENTRY_SIZE_BYTES = 32;
    private static final int CHUNK_ENTRY_SIZE_BYTES = 32;

    private final FileChannel channel;
    private final Compressor compressor;
    private final List<FileInfo> input;

    public ArchiveWriter(@NotNull Path path, @NotNull Compressor compressor) throws IOException {
        this.channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        this.compressor = compressor;
        this.input = new ArrayList<>();
    }

    public void append(@NotNull String path, @NotNull ByteBuffer entry) {
        input.add(new FileInfo(entry, path));
    }

    @Override
    public void close() throws IOException {
        final List<FileEntry> files = new ArrayList<>();
        final List<ChunkEntry> chunks = new ArrayList<>();

        channel.position(getHeaderSize(input));

        writeFiles(files, chunks);

        channel.position(0);

        writeHeader(files, chunks);

        channel.close();
    }

    private void writeFiles(@NotNull List<FileEntry> files, @NotNull List<ChunkEntry> chunks) throws IOException {
        final Deque<FileInfo> queue = new ArrayDeque<>(input);
        final ByteBuffer acc = ByteBuffer.allocate(Compressor.BLOCK_SIZE_BYTES);

        long fileDataOffset = 0;
        long chunkDataOriginalOffset = 0;
        long chunkDataCompressedOffset = channel.position();

        while (!queue.isEmpty()) {
            acc.clear();

            while (acc.hasRemaining() && !queue.isEmpty()) {
                final FileInfo info = queue.element();
                final ByteBuffer src = info.buffer();
                final int length = Math.min(src.remaining(), acc.remaining());

                acc.put(acc.position(), src, src.position(), length);
                acc.position(acc.position() + length);
                src.position(src.position() + length);

                if (!src.hasRemaining()) {
                    files.add(new FileEntry(info, new EntrySpan(fileDataOffset, info.buffer().limit())));
                    fileDataOffset += info.buffer().limit();
                    queue.remove();
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

        final int headerSize = getHeaderSize(input);

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
            buffer.putLong(ArchiveManager.hashFileName(file.info().path()));

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
    }

    private int getHeaderSize(@NotNull List<FileInfo> files) {
        return HEADER_SIZE_BYTES
            + FILE_ENTRY_SIZE_BYTES * files.size()
            + CHUNK_ENTRY_SIZE_BYTES * getChunksCount(files);
    }

    private int getChunksCount(@NotNull List<FileInfo> files) {
        final int size = files.stream()
            .mapToInt(file -> file.buffer().limit())
            .sum();

        return Compressor.getBlocksCount(size);
    }

    private static record FileInfo(@NotNull ByteBuffer buffer, @NotNull String path) {
    }

    private static record FileEntry(@NotNull FileInfo info, @NotNull EntrySpan span) {
    }

    private static record ChunkEntry(@NotNull EntrySpan original, @NotNull EntrySpan compressed) {
    }

    private static record EntrySpan(long offset, int length) {
    }
}
