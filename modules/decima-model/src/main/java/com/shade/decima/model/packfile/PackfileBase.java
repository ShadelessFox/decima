package com.shade.decima.model.packfile;

import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class PackfileBase {
    public static final long[] HEADER_KEY = {0xF41CAB62FA3A9443L, 0xD2A89E3EF376811CL};
    public static final long[] DATA_KEY = {0x7E159D956C084A37L, 0x18AA7D3F3D5AF7E8L};

    public static final int MAGIC_PLAIN = 0x20304050;
    public static final int MAGIC_ENCRYPTED = 0x21304050;

    protected Header header;

    /**
     * {@link FileEntry#hash()} to {@link FileEntry} mappings.
     */
    protected final SortedMap<Long, FileEntry> files;

    /**
     * {@link Span#offset()} of {@link ChunkEntry#decompressed()} to {@link ChunkEntry} mappings.
     */
    protected final NavigableMap<Long, ChunkEntry> chunks;

    protected PackfileBase() {
        this.files = new TreeMap<>(Long::compareUnsigned);
        this.chunks = new TreeMap<>(Long::compareUnsigned);
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

    public boolean isEmpty() {
        return header.fileEntryCount() == 0;
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
        final byte[] data = path.getBytes(StandardCharsets.UTF_8);
        final byte[] cstr = Arrays.copyOf(data, data.length + 1);
        return MurmurHash3.mmh3(cstr)[0];
    }

    private static void swizzle(@NotNull ByteBuffer target, int key1, int key2) {
        final ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer slice = target.slice().order(ByteOrder.LITTLE_ENDIAN);

        buffer.putLong(0, HEADER_KEY[0]);
        buffer.putLong(8, HEADER_KEY[1]);
        buffer.putInt(0, key1);

        final long[] hash1 = MurmurHash3.mmh3(buffer.array(), 0, 16);
        slice.putLong(0, slice.getLong(0) ^ hash1[0]);
        slice.putLong(8, slice.getLong(8) ^ hash1[1]);

        buffer.putLong(0, HEADER_KEY[0]);
        buffer.putLong(8, HEADER_KEY[1]);
        buffer.putInt(0, key2);

        final long[] hash2 = MurmurHash3.mmh3(buffer.array(), 0, 16);
        slice.putLong(16, slice.getLong(16) ^ hash2[0]);
        slice.putLong(24, slice.getLong(24) ^ hash2[1]);
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

                PackfileBase.swizzle(buffer, key1, key2);

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
                PackfileBase.swizzle(buffer.slice(base, 32), decompressed.key, compressed.key);

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

            final long[] hash1 = MurmurHash3.mmh3(buffer.array());
            buffer.putLong(0, hash1[0] ^ DATA_KEY[0]);
            buffer.putLong(8, hash1[1] ^ DATA_KEY[1]);

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
}
