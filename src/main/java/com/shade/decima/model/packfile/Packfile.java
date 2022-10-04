package com.shade.decima.model.packfile;

import com.shade.decima.model.util.Compressor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

public class Packfile extends PackfileBase implements Closeable, Comparable<Packfile> {
    private final SeekableByteChannel channel;
    private final Compressor compressor;
    private final PackfileInfo info;
    private final Path path;

    public Packfile(@NotNull SeekableByteChannel channel, @NotNull Compressor compressor, @Nullable PackfileInfo info, @NotNull Path path) throws IOException {
        super(Header.read(IOUtils.readExact(channel, Header.BYTES)));

        this.channel = channel;
        this.compressor = compressor;
        this.info = info;
        this.path = path;

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
        return newInputStream(path).readAllBytes();
    }

    @NotNull
    public byte[] extract(long hash) throws IOException {
        return newInputStream(hash).readAllBytes();
    }

    @NotNull
    public InputStream newInputStream(@NotNull String path) {
        return newInputStream(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public InputStream newInputStream(long hash) {
        final FileEntry entry = getFileEntry(hash);

        if (entry == null) {
            throw new IllegalArgumentException("Can't find path 0x" + Long.toHexString(hash) + " in this archive");
        }

        return new PackfileInputStream(entry);
    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    public String getName() {
        return info != null ? info.getName() : path.getFileName().toString();
    }

    @Nullable
    public PackfileInfo getInfo() {
        return info;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int compareTo(Packfile o) {
        return Comparator.comparing(Path::getFileName).compare(path, o.path);
    }

    private class PackfileInputStream extends InputStream {
        private final FileEntry file;
        private final ChunkEntry[] chunks;
        private final byte[] srcbuf;
        private final byte[] dstbuf;

        private int dataoff;
        private int datalen;
        private int chunkidx;

        public PackfileInputStream(@NotNull FileEntry file) {
            this.file = file;
            this.chunks = getChunkEntries(file.span()).values().toArray(ChunkEntry[]::new);
            this.srcbuf = new byte[Compressor.getCompressedSize(header.chunkEntrySize())];
            this.dstbuf = new byte[header.chunkEntrySize()];
        }

        @Override
        public int read() throws IOException {
            final byte[] buf = new byte[1];
            return read(buf, 0, 1) > 0 ? buf[0] : -1;
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            final byte[] buffer = new byte[file.span().size()];

            if (read(buffer) != buffer.length) {
                throw new IOException("Buffer underflow");
            }

            return buffer;
        }

        @Override
        public int read(@NotNull byte[] buf, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, buf.length);

            if (len == 0) {
                return 0;
            }

            int read = 0;

            while (read < len) {
                if (datalen < 0) {
                    if (chunkidx >= chunks.length) {
                        break;
                    }

                    final ChunkEntry chunk = chunks[chunkidx];
                    final ByteBuffer buffer = ByteBuffer.wrap(srcbuf, 0, chunk.compressed().size());

                    synchronized (channel) {
                        channel.position(chunk.compressed().offset());
                        channel.read(buffer.slice());
                    }

                    if (header.isEncrypted()) {
                        chunk.swizzle(buffer.slice());
                    }

                    compressor.decompress(srcbuf, chunk.compressed().size(), dstbuf, chunk.decompressed().size());
                    dataoff = 0;
                    datalen = chunk.decompressed().size();

                    if (chunkidx == 0) {
                        dataoff = (int) (file.span().offset() - chunk.decompressed().offset());
                    }

                    if (chunkidx == chunks.length - 1) {
                        datalen = (int) (file.span().size() - chunk.decompressed().offset() + file.span().offset());
                    }

                    chunkidx += 1;
                }

                final int length = Math.min(datalen - dataoff, len - read);

                if (length > 0) {
                    System.arraycopy(dstbuf, dataoff, buf, off + read, length);
                    read += length;
                    dataoff += length;
                } else {
                    datalen = -1;
                }
            }

            return read > 0 ? read : -1;
        }
    }
}
