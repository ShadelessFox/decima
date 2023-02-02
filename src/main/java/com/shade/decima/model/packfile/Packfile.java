package com.shade.decima.model.packfile;

import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.event.EventListenerList;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Packfile extends PackfileBase implements Closeable, Comparable<Packfile> {
    private SeekableByteChannel channel;
    private final Compressor compressor;
    private final PackfileInfo info;
    private final Path path;
    private final Map<FilePath, Change> changes = new HashMap<>();
    private final EventListenerList listeners = new EventListenerList();

    public Packfile(@NotNull Path path, @NotNull Compressor compressor, @Nullable PackfileInfo info) throws IOException {
        this.compressor = compressor;
        this.info = info;
        this.path = path;

        read();
        validate();
    }

    public synchronized void reload(boolean purgeChanges) throws IOException {
        if (purgeChanges) {
            clearChanges();
        }

        close();

        header = null;
        files.clear();
        chunks.clear();

        read();
        validate();
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
    public InputStream newInputStream(@NotNull String path) throws IOException {
        return newInputStream(getPathHash(getNormalizedPath(path)));
    }

    @NotNull
    public InputStream newInputStream(long hash) throws IOException {
        ensureOpen();

        final Change change = getChange(hash);
        if (change != null) {
            return new ResourceInputStream(change.toResource());
        }

        final FileEntry entry = getFileEntry(hash);
        if (entry == null) {
            throw new IllegalArgumentException("Can't find path 0x" + Long.toHexString(hash) + " in this archive");
        }

        return new PackfileInputStream(entry);
    }

    @NotNull
    public Map<FilePath, Change> getChanges() {
        return changes;
    }

    @Nullable
    public Change getChange(long hash) {
        for (Map.Entry<FilePath, Change> change : changes.entrySet()) {
            if (change.getKey().hash() == hash) {
                return change.getValue();
            }
        }

        return null;
    }

    public void addChange(@NotNull FilePath path, @NotNull Change change) {
        changes.put(path, change);

        for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
            listener.fileChanged(this, path, change);
        }
    }

    public void removeChange(@NotNull FilePath path) {
        final Change change = changes.remove(path);

        if (change != null) {
            for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
                listener.fileChanged(this, path, change);
            }
        }
    }

    public void clearChanges() {
        for (Map.Entry<FilePath, Change> change : changes.entrySet()) {
            for (PackfileChangeListener listener : listeners.getListeners(PackfileChangeListener.class)) {
                listener.fileChanged(this, change.getKey(), change.getValue());
            }
        }

        changes.clear();
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public boolean hasChange(@NotNull FilePath path) {
        return changes.containsKey(path);
    }

    public boolean hasChangesInPath(@NotNull FilePath other) {
        for (FilePath path : changes.keySet()) {
            if (path.startsWith(other)) {
                return true;
            }
        }

        return false;
    }

    public void addChangeListener(@NotNull PackfileChangeListener listener) {
        listeners.add(PackfileChangeListener.class, listener);
    }

    public void removeChangeListener(@NotNull PackfileChangeListener listener) {
        listeners.remove(PackfileChangeListener.class, listener);
    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    public String getName() {
        return info != null ? info.name() : path.getFileName().toString();
    }

    @Nullable
    public PackfileInfo getInfo() {
        return info;
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    public int compareTo(Packfile o) {
        return Comparator.comparing(Path::getFileName).compare(path, o.path);
    }

    private void read() throws IOException {
        channel = Files.newByteChannel(path, StandardOpenOption.READ);
        header = Header.read(IOUtils.readExact(channel, Header.BYTES));

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

    private void validate() throws IOException {
        final long actualHeaderSize = Header.BYTES + header.fileEntryCount() * FileEntry.BYTES + (long) header.chunkEntryCount() * ChunkEntry.BYTES;
        long actualFileSize = actualHeaderSize;
        long actualDataSize = 0;

        for (ChunkEntry entry : chunks.values()) {
            actualFileSize += entry.compressed().size();
            actualDataSize += entry.decompressed().size();
        }

        if (channel.size() != header.fileSize()) {
            throw new IOException("File size does not match the physical size (expected: " + channel.size() + ", actual: " + header.fileSize() + ")");
        }

        if (actualFileSize != header.fileSize()) {
            throw new IOException("File size does not match the actual size (expected: " + actualFileSize + ", actual: " + header.fileSize() + ")");
        }

        if (actualDataSize != header.dataSize()) {
            throw new IOException("Data size does not match the actual size (expected: " + actualDataSize + ", actual: " + header.dataSize() + ")");
        }

        if (Compressor.BLOCK_SIZE_BYTES != header.chunkEntrySize()) {
            throw new IOException("Unexpected maximum chunk size (expected: " + Compressor.BLOCK_SIZE_BYTES + ", actual: " + header.chunkEntrySize() + ")");
        }

        Span lastCompressedSpan = null;
        Span lastDecompressedSpan = null;

        for (ChunkEntry entry : chunks.values()) {
            if (entry.compressed().offset() < actualHeaderSize || entry.compressed().offset() + entry.compressed().size() > actualFileSize) {
                throw new IOException("Invalid compressed chunk span: " + entry);
            }

            if (entry.decompressed().offset() + entry.decompressed().size() > actualDataSize) {
                throw new IOException("Invalid decompressed chunk span: " + entry);
            }

            if (lastCompressedSpan != null && lastCompressedSpan.offset() + lastCompressedSpan.size() != entry.compressed().offset()) {
                throw new IOException("Compressed data span contains gaps or entries are unsorted: " + entry);
            }

            if (lastDecompressedSpan != null && lastDecompressedSpan.offset() + lastDecompressedSpan.size() != entry.decompressed().offset()) {
                throw new IOException("Decompressed data span contains gaps or entries are unsorted: " + entry);
            }

            lastCompressedSpan = entry.compressed();
            lastDecompressedSpan = entry.decompressed();
        }

        for (FileEntry entry : files.values()) {
            if (entry.span().offset() + entry.span().size() > actualDataSize) {
                throw new IOException("File span is bigger than actual data size: " + entry);
            }
        }
    }

    private void ensureOpen() throws IOException {
        if (channel != null && !channel.isOpen()) {
            reload(false);
        }
    }

    private static class ResourceInputStream extends InputStream {
        private final Resource resource;

        public ResourceInputStream(@NotNull Resource resource) {
            this.resource = resource;
        }

        @Override
        public int read() throws IOException {
            final byte[] buffer = new byte[1];
            final int length = (int) resource.read(ByteBuffer.wrap(buffer));

            if (length < 0) {
                return -1;
            } else {
                return buffer[0] & 0xff;
            }
        }

        @Override
        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            return (int) resource.read(ByteBuffer.wrap(b, off, len));
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            final byte[] buffer = new byte[resource.size()];
            final int length = (int) resource.read(ByteBuffer.wrap(buffer));
            return Arrays.copyOf(buffer, length);
        }
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

                    synchronized (Packfile.this) {
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
