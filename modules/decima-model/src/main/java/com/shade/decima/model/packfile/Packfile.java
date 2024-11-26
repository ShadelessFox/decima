package com.shade.decima.model.packfile;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.FilePath;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.model.util.BufferUtils;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import com.shade.util.hash.Hashing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Packfile implements Archive, Comparable<Packfile> {
    public static final Topic<PackfileChangeListener> CHANGES = Topic.create("packfile", PackfileChangeListener.class);

    public static final long[] HEADER_KEY = {0xF41CAB62FA3A9443L, 0xD2A89E3EF376811CL};
    public static final long[] DATA_KEY = {0x7E159D956C084A37L, 0x18AA7D3F3D5AF7E8L};
    public static final int MAGIC_PLAIN = 0x20304050;
    public static final int MAGIC_ENCRYPTED = 0x21304050;
    public static final int MAXIMUM_BLOCK_SIZE = 0x40000;

    private final PackfileManager manager;
    private final Compressor compressor;
    private final PackfileInfo info;
    private final Map<FilePath, Change> changes = new HashMap<>();

    private Header header;
    /** {@link FileEntry#hash()} to {@link FileEntry} mappings. */
    private final SortedMap<Long, FileEntry> files = new TreeMap<>(Long::compareUnsigned);
    /** {@link Span#offset()} of {@link ChunkEntry#decompressed()} to {@link ChunkEntry} mappings. */
    private final NavigableMap<Long, ChunkEntry> chunks = new TreeMap<>(Long::compareUnsigned);
    private SeekableByteChannel channel;

    Packfile(@NotNull PackfileManager manager, @NotNull Compressor compressor, @NotNull PackfileInfo info) throws IOException {
        this.manager = manager;
        this.compressor = compressor;
        this.info = info;

        read();
        validate();
    }

    @NotNull
    @Override
    public PackfileManager getManager() {
        return manager;
    }

    @Nullable
    public FileEntry getFileEntry(@NotNull String path) {
        return files.get(getPathHash(getNormalizedPath(path)));
    }

    @Nullable
    public FileEntry getFileEntry(long hash) {
        return files.get(hash);
    }

    @NotNull
    public Collection<FileEntry> getFileEntries() {
        return files.values();
    }

    @NotNull
    public NavigableMap<Long, ChunkEntry> getChunkEntries(@NotNull Span span) {
        final NavigableMap<Long, ChunkEntry> map = chunks.subMap(
            chunks.floorKey(span.offset()), true,
            chunks.floorKey(span.offset() + span.size()), true
        );

        if (map.isEmpty()) {
            throw new IllegalArgumentException(String.format("Can't find any chunk entries for span starting at %#x (size: %#x)", span.offset(), span.size()));
        }

        assert map.firstEntry().getValue().decompressed().contains(span.offset());
        assert map.lastEntry().getValue().decompressed().contains(span.offset() + span.size());

        return map;
    }

    public boolean contains(long hash) {
        return files.containsKey(hash);
    }

    @NotNull
    public static String getNormalizedPath(@NotNull String path) {
        return getNormalizedPath(path, true);
    }

    @NotNull
    public static String getNormalizedPath(@NotNull String path, boolean normalizeExtension) {
        if (path.isEmpty()) {
            return path;
        }

        path = path.replace("\\", "/");

        while (!path.isEmpty() && path.charAt(0) == '/') {
            path = path.substring(1);
        }

        if (normalizeExtension) {
            final String extension = IOUtils.getExtension(path);

            if (!extension.equals("core") && !extension.equals("stream")) {
                path += ".core";
            }
        }

        return path;
    }

    public static long getPathHash(@NotNull String path) {
        return Hashing.decimaMurmur3().hashString(path + '\0').asLong();
    }

    private static void swizzle(@NotNull ByteBuffer target, int key1, int key2) {
        final ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer slice = target.slice().order(ByteOrder.LITTLE_ENDIAN);

        buffer.putLong(0, HEADER_KEY[0]);
        buffer.putLong(8, HEADER_KEY[1]);
        buffer.putInt(0, key1);

        var hash1 = Hashing.decimaMurmur3().hashBytes(buffer.array(), 0, 16).asBuffer();
        slice.putLong(0, slice.getLong(0) ^ hash1.getLong());
        slice.putLong(8, slice.getLong(8) ^ hash1.getLong());

        buffer.putLong(0, HEADER_KEY[0]);
        buffer.putLong(8, HEADER_KEY[1]);
        buffer.putInt(0, key2);

        var hash2 = Hashing.decimaMurmur3().hashBytes(buffer.array(), 0, 16).asBuffer();
        slice.putLong(16, slice.getLong(16) ^ hash2.getLong());
        slice.putLong(24, slice.getLong(24) ^ hash2.getLong());
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
            throw new IOException("Can't find file %#018x in %s".formatted(hash, getPath()));
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
        MessageBus.getInstance().publisher(CHANGES).fileChanged(this, path, change);
    }

    public void removeChange(@NotNull FilePath path) {
        final Change change = changes.remove(path);
        if (change == null) {
            return;
        }
        MessageBus.getInstance().publisher(CHANGES).fileChanged(this, path, change);
    }

    public void clearChanges() {
        for (Map.Entry<FilePath, Change> change : changes.entrySet()) {
            MessageBus.getInstance().publisher(CHANGES).fileChanged(this, change.getKey(), change.getValue());
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

    @Nullable
    @Override
    public ArchiveFile findFile(@NotNull String identifier) {
        final FileEntry entry = getFileEntry(identifier);
        if (entry == null) {
            return null;
        }
        return new PackfileFile(this, entry);
    }

    @Nullable
    @Override
    public ArchiveFile findFile(long identifier) {
        final FileEntry entry = getFileEntry(identifier);
        if (entry == null) {
            return null;
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

    protected void read() throws IOException {
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

    protected void validate() throws IOException {
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

        if (MAXIMUM_BLOCK_SIZE != header.chunkEntrySize()) {
            throw new IOException("Unexpected maximum chunk size (expected: " + MAXIMUM_BLOCK_SIZE + ", actual: " + header.chunkEntrySize() + ")");
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

    public record Header(int magic, int key, long fileSize, long dataSize, long fileEntryCount, int chunkEntryCount, int chunkEntrySize) {
        public static final int BYTES = 40;

        @NotNull
        public static Header read(@NotNull ByteBuffer buffer) throws IOException {
            assert buffer.remaining() >= BYTES;

            final var magic = buffer.getInt();
            final var key = buffer.getInt();

            if (magic != MAGIC_PLAIN && magic != MAGIC_ENCRYPTED) {
                throw new IOException("File magic is invalid, expected %x or %x, got %d".formatted(MAGIC_PLAIN, MAGIC_ENCRYPTED, magic));
            }

            if (isEncrypted(magic)) {
                swizzle(buffer, key, key + 1);
            }

            final var fileSize = buffer.getLong();
            final var dataSize = buffer.getLong();
            final var fileEntryCount = buffer.getLong();
            final var chunkEntryCount = buffer.getInt();
            final var chunkEntrySize = buffer.getInt();

            return new Header(magic, key, fileSize, dataSize, fileEntryCount, chunkEntryCount, chunkEntrySize);
        }

        public void write(@NotNull ByteBuffer buffer) {
            assert buffer.remaining() >= BYTES;

            buffer.putInt(magic);
            buffer.putInt(key);

            final int position = buffer.position();

            buffer.putLong(fileSize);
            buffer.putLong(dataSize);
            buffer.putLong(fileEntryCount);
            buffer.putInt(chunkEntryCount);
            buffer.putInt(chunkEntrySize);

            if (isEncrypted()) {
                swizzle(buffer.slice(position, 32), key, key + 1);
            }
        }

        public static boolean isEncrypted(int magic) {
            return magic == MAGIC_ENCRYPTED;
        }

        public boolean isEncrypted() {
            return isEncrypted(magic);
        }
    }

    public record FileEntry(int index, int key, long hash, @NotNull Span span) implements Comparable<FileEntry> {
        public static final int BYTES = 32;

        @NotNull
        public static FileEntry read(@NotNull ByteBuffer buffer, boolean encrypted) {
            assert buffer.remaining() >= BYTES;

            if (encrypted) {
                final int base = buffer.position();
                final int key1 = buffer.getInt(base + 4);
                final int key2 = buffer.getInt(base + 28);

                swizzle(buffer, key1, key2);

                buffer.putInt(base + 4, key1);
                buffer.putInt(base + 28, key2);
            }

            final var index = buffer.getInt();
            final var key = buffer.getInt();
            final var hash = buffer.getLong();
            final var span = Span.read(buffer);

            return new FileEntry(index, key, hash, span);
        }

        public void write(@NotNull ByteBuffer buffer, boolean encrypt) {
            assert buffer.remaining() >= BYTES;

            final int base = buffer.position();

            buffer.putInt(index);
            buffer.putInt(key);
            buffer.putLong(hash);
            span.write(buffer);

            if (encrypt) {
                swizzle(buffer.slice(base, 32), key, span.key);

                buffer.putInt(base + 4, key);
                buffer.putInt(base + 28, span.key);
            }
        }

        @Override
        public int compareTo(FileEntry o) {
            return Long.compareUnsigned(hash, o.hash);
        }
    }

    public record ChunkEntry(@NotNull Span decompressed, @NotNull Span compressed) implements Comparable<ChunkEntry> {
        public static final int BYTES = 32;

        private static final ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });

        public static ChunkEntry read(@NotNull ByteBuffer buffer, boolean encrypted) {
            assert buffer.remaining() >= BYTES;

            if (encrypted) {
                final int base = buffer.position();
                final int key1 = buffer.getInt(base + 12);
                final int key2 = buffer.getInt(base + 28);

                Packfile.swizzle(buffer, key1, key2);

                buffer.putInt(base + 12, key1);
                buffer.putInt(base + 28, key2);
            }

            final var uncompressed = Span.read(buffer);
            final var compressed = Span.read(buffer);

            return new ChunkEntry(uncompressed, compressed);
        }

        public void write(@NotNull ByteBuffer buffer, boolean encrypt) {
            assert buffer.remaining() >= BYTES;

            final int base = buffer.position();

            decompressed.write(buffer);
            compressed.write(buffer);

            if (encrypt) {
                Packfile.swizzle(buffer.slice(base, 32), decompressed.key, compressed.key);

                buffer.putInt(base + 12, decompressed.key);
                buffer.putInt(base + 28, compressed.key);
            }
        }

        public void swizzle(@NotNull ByteBuffer buffer) {
            swizzle(buffer, decompressed);
        }

        public static void swizzle(@NotNull ByteBuffer target, @NotNull Span decompressed) {
            final ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(decompressed.offset);
            buffer.putInt(decompressed.size);
            buffer.putInt(decompressed.key);

            var hash1 = Hashing.decimaMurmur3().hashBytes(buffer.array()).asBuffer();
            buffer.putLong(0, hash1.getLong() ^ DATA_KEY[0]);
            buffer.putLong(8, hash1.getLong() ^ DATA_KEY[1]);

            final byte[] hash2 = MD5.get().digest(buffer.array());
            buffer.put(0, hash2);

            final ByteBuffer slice = target.slice().order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0, limit = slice.limit() & ~7; i < limit; i += 8) {
                // Process 8 bytes at a time
                slice.putLong(i, slice.getLong(i) ^ buffer.getLong(i & 15));
            }
            for (int i = slice.limit() & ~7, limit = slice.limit(); i < limit; i++) {
                // Process remaining bytes
                slice.put(i, (byte) (slice.get(i) ^ buffer.get(i & 15)));
            }
        }

        @Override
        public int compareTo(ChunkEntry o) {
            return Long.compareUnsigned(decompressed.offset, o.decompressed.offset);
        }
    }

    public record Span(long offset, int size, int key) implements Comparable<Span> {
        public static final int BYTES = 16;

        @NotNull
        public static Span read(@NotNull ByteBuffer buffer) {
            assert buffer.remaining() >= BYTES;

            final var offset = buffer.getLong();
            final var size = buffer.getInt();
            final var key = buffer.getInt();

            return new Span(offset, size, key);
        }

        public void write(@NotNull ByteBuffer buffer) {
            assert buffer.remaining() >= BYTES;

            buffer.putLong(offset);
            buffer.putInt(size);
            buffer.putInt(key);
        }

        public boolean contains(long offset) {
            return offset >= this.offset && offset <= this.offset + size;
        }

        @Override
        public int compareTo(@NotNull Span other) {
            return Long.compare(offset, other.offset);
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
            final int length = resource.read(ByteBuffer.wrap(buffer));

            if (length <= 0) {
                return -1;
            } else {
                return buffer[0] & 0xff;
            }
        }

        @Override
        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            return resource.read(ByteBuffer.wrap(b, off, len));
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            final byte[] buffer = new byte[resource.size()];
            final int length = resource.read(ByteBuffer.wrap(buffer));
            return Arrays.copyOf(buffer, length);
        }
    }

    private class PackfileInputStream extends InputStream {
        private final FileEntry file;
        private final ChunkEntry[] chunks;

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
            final ByteBuffer src = ByteBuffer.allocate(chunk.compressed().size());
            final ByteBuffer dst = ByteBuffer.wrap(decompressed, 0, chunk.decompressed().size());

            try {
                synchronized (Packfile.this) {
                    channel.position(chunk.compressed().offset());
                    channel.read(src.slice());
                }

                if (header.isEncrypted()) {
                    chunk.swizzle(src.slice());
                }

                compressor.decompress(src, dst);
            } catch (Exception e) {
                throw new IOException("Error reading chunk %d of file %#18x in %s".formatted(index, file.hash(), getPath()), e);
            }

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
