package com.shade.decima.model.packfile;

import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;

public class Packfile extends PackfileBase implements Closeable, Comparable<Packfile> {
    private final Path path;
    private final String name;
    private final FileChannel channel;
    private final Compressor compressor;

    public Packfile(@NotNull Path path, @Nullable String name, @NotNull FileChannel channel, @NotNull Compressor compressor) throws IOException {
        super(Header.read(IOUtils.readExact(channel, Header.BYTES)));

        this.path = path;
        this.name = name;
        this.channel = channel;
        this.compressor = compressor;

        for (long i = 0; i < header.fileEntryCount(); i++) {
            final ByteBuffer buffer = IOUtils.readExact(channel, FileEntry.BYTES);
            final FileEntry entry = FileEntry.read(buffer, header.isEncrypted());
            files.put(entry.hash(), entry);
        }

        for (long i = 0; i < header.chunkEntryCount(); i++) {
            final ByteBuffer buffer = IOUtils.readExact(channel, ChunkEntry.BYTES);
            final ChunkEntry entry = ChunkEntry.read(buffer, header.isEncrypted());
            chunks.put(entry.decompressed().offset(), entry);
        }
    }

    @NotNull
    public byte[] extract(@NotNull String path) throws IOException {
        return extract(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public byte[] extract(long hash) throws IOException {
        final FileEntry entry = getFileEntry(hash);
        if (entry == null) {
            throw new IllegalArgumentException("Can't find path 0x" + Long.toHexString(hash) + " in this archive");
        }
        return extract(entry);
    }

    @NotNull
    private byte[] extract(@NotNull FileEntry file) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(file.span().size());
        final NavigableMap<Long, ChunkEntry> entries = getChunkEntries(file.span());

        for (Map.Entry<Long, ChunkEntry> entry : entries.entrySet()) {
            final ChunkEntry chunk = entry.getValue();

            final ByteBuffer chunkBufferCompressed = ByteBuffer.allocate(chunk.compressed().size());
            channel.position(chunk.compressed().offset());
            channel.read(chunkBufferCompressed.slice());

            if (header.isEncrypted()) {
                chunk.swizzle(chunkBufferCompressed);
            }

            final ByteBuffer chunkBufferDecompressed = ByteBuffer.allocate(chunk.decompressed().size());
            compressor.decompress(chunkBufferCompressed.array(), chunkBufferDecompressed.array());

            if (entry.equals(entries.firstEntry())) {
                final int offset = (int) (file.span().offset() & (header.chunkEntrySize() - 1));
                final int length = Math.min(buffer.remaining(), chunkBufferDecompressed.remaining() - offset);
                buffer.put(chunkBufferDecompressed.slice(offset, length));
            } else if (entry.equals(entries.lastEntry())) {
                final int offset = 0;
                final int length = Math.min(buffer.remaining(), chunkBufferDecompressed.remaining() - offset);
                buffer.put(chunkBufferDecompressed.slice(offset, length));
            } else {
                buffer.put(chunkBufferDecompressed);
            }
        }

        if (buffer.remaining() > 0) {
            throw new IOException("Buffer underflow");
        }

        final byte[] result = new byte[file.span().size()];
        buffer.position(0);
        buffer.get(result);

        return result;
    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    public String getName() {
        return name != null ? name : path.getFileName().toString();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int compareTo(Packfile o) {
        return Comparator.comparing(Path::getFileName).compare(path, o.path);
    }
}
