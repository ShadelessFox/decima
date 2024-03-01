package com.shade.decima.model.packfile;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.model.util.Oodle;
import com.shade.platform.model.util.BufferUtils;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Packfile extends PackfileBase implements Archive, Comparable<Packfile> {
    private SeekableByteChannel channel;
    private final Oodle oodle;
    private final PackfileInfo info;
    private final Map<FilePath, Change> changes = new HashMap<>();
    private final EventListenerList listeners = new EventListenerList();

    public Packfile(@NotNull Path path, @NotNull Oodle oodle) throws IOException {
        this(new PackfileInfo(path, IOUtils.getBasename(path), null), oodle);
    }

    Packfile(@NotNull PackfileInfo info, @NotNull Oodle oodle) throws IOException {
        this.oodle = oodle;
        this.info = info;

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
    @Override
    public String getId() {
        return info.path().getFileName().toString();
    }

    @NotNull
    @Override
    public Path getPath() {
        return info.path();
    }

    @NotNull
    @Override
    public String getName() {
        return info.name();
    }

    @NotNull
    @Override
    public ArchiveFile getFile(@NotNull String identifier) {
        final FileEntry entry = getFileEntry(identifier);
        if (entry == null) {
            throw new IllegalArgumentException("Can't find file '%s' in archive %s".formatted(identifier, getName()));
        }
        return new PackfileFile(this, entry);
    }

    @NotNull
    public ArchiveFile getFile(long hash) {
        final FileEntry entry = getFileEntry(hash);
        if (entry == null) {
            throw new IllegalArgumentException("Can't find file '?#%016x' in archive %s".formatted(hash, getName()));
        }
        return new PackfileFile(this, entry);
    }

    @Nullable
    public String getLanguage() {
        return info.language();
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }

    @Override
    public int compareTo(@NotNull Packfile o) {
        final String name1 = getName().toLowerCase(Locale.ROOT);
        final String name2 = o.getName().toLowerCase(Locale.ROOT);

        int cmp = Boolean.compare(name1.startsWith("patch"), name2.startsWith("patch"));

        if (cmp == 0) {
            cmp = name1.compareTo(name2);
        }

        if (cmp == 0) {
            final String lang1 = Objects.requireNonNullElse(getLanguage(), "");
            final String lang2 = Objects.requireNonNullElse(o.getLanguage(), "");
            cmp = lang1.compareTo(lang2);
        }

        return cmp;
    }

    @Override
    public String toString() {
        return "Packfile[" + info.path() + ']';
    }

    private void read() throws IOException {
        channel = Files.newByteChannel(info.path(), StandardOpenOption.READ);
        header = Header.read(BufferUtils.readFromChannel(channel, Header.BYTES));

        final ByteBuffer buffer = BufferUtils.readFromChannel(
            channel,
            (int) header.fileEntryCount() * FileEntry.BYTES + header.chunkEntryCount() * ChunkEntry.BYTES
        );

        for (long i = 0; i < header.fileEntryCount(); i++) {
            final FileEntry entry = FileEntry.read(buffer, header.isEncrypted());
            files.put(entry.hash(), entry);
        }

        for (long i = 0; i < header.chunkEntryCount(); i++) {
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

        if (Oodle.BLOCK_SIZE_BYTES != header.chunkEntrySize()) {
            throw new IOException("Unexpected maximum chunk size (expected: " + Oodle.BLOCK_SIZE_BYTES + ", actual: " + header.chunkEntrySize() + ")");
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

            if (length <= 0) {
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

        private final byte[] compressed = new byte[Oodle.getCompressedSize(header.chunkEntrySize())];
        private final byte[] decompressed = new byte[header.chunkEntrySize()];

        private int index; // index of the current chunk
        private int count; // count of bytes in the current chunk
        private int pos; // position in the current chunk

        public PackfileInputStream(@NotNull FileEntry file) {
            this.file = file;
            this.chunks = getChunkEntries(file.span()).values().toArray(ChunkEntry[]::new);
        }

        @Override
        public int read() throws IOException {
            if (pos >= count) {
                fill();
            }
            if (pos >= count) {
                return -1;
            }
            return decompressed[pos++] & 0xff;
        }

        @Override
        public int read(@NotNull byte[] buf, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, buf.length);

            if (len == 0) {
                return 0;
            }

            for (int n = 0; ; ) {
                final int nread = read1(buf, off + n, len - n);
                if (nread <= 0) {
                    return n == 0 ? nread : n;
                }
                n += nread;
                if (n >= len) {
                    return n;
                }
            }
        }

        private int read1(@NotNull byte[] buf, int off, int len) throws IOException {
            int available = count - pos;

            if (available <= 0) {
                fill();
                available = count - pos;
            }

            if (available <= 0) {
                return -1;
            }

            final int count = Math.min(available, len);
            System.arraycopy(decompressed, pos, buf, off, count);
            pos += count;

            return count;
        }

        private void fill() throws IOException {
            if (index >= chunks.length) {
                return;
            }

            final ChunkEntry chunk = chunks[index];
            final ByteBuffer buffer = ByteBuffer.wrap(compressed, 0, chunk.compressed().size());

            synchronized (Packfile.this) {
                channel.position(chunk.compressed().offset());
                channel.read(buffer.slice());
            }

            if (header.isEncrypted()) {
                chunk.swizzle(buffer.slice());
            }

            oodle.decompress(compressed, chunk.compressed().size(), decompressed, chunk.decompressed().size());

            if (index == 0) {
                pos = (int) (file.span().offset() - chunk.decompressed().offset());
            } else {
                pos = 0;
            }

            if (index == chunks.length - 1) {
                count = (int) (file.span().offset() + file.span().size() - chunk.decompressed().offset());
            } else {
                count = chunk.decompressed().size();
            }

            index++;
        }
    }
}
