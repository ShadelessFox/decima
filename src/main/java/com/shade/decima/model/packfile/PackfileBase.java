package com.shade.decima.model.packfile;

import com.shade.decima.model.util.Compressor;
import com.shade.decima.model.util.hash.MurmurHash3;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class PackfileBase {
    public static final int[] HEADER_KEY = new int[]{0x0FA3A9443, 0x0F41CAB62, 0x0F376811C, 0x0D2A89E3E};
    public static final int[] DATA_KEY = new int[]{0x06C084A37, 0x07E159D95, 0x03D5AF7E8, 0x018AA7D3F};

    public static final int MAGIC_PLAIN = 0x20304050;
    public static final int MAGIC_ENCRYPTED = 0x21304050;

    protected final Header header;

    /**
     * {@link FileEntry#hash()} to {@link FileEntry} mappings.
     */
    protected final SortedMap<Long, FileEntry> files;

    /**
     * {@link Span#offset()} of {@link ChunkEntry#decompressed()} to {@link ChunkEntry} mappings.
     */
    protected final NavigableMap<Long, ChunkEntry> chunks;

    protected PackfileBase(@NotNull Header header) {
        this.header = header;
        this.files = new TreeMap<>(Long::compareUnsigned);
        this.chunks = new TreeMap<>(Long::compareUnsigned);
    }

    @Nullable
    public FileEntry getFileEntry(long hash) {
        return files.get(hash);
    }

    @NotNull
    public Collection<FileEntry> getFileEntries() {
        return files.values();
    }

    @Nullable
    public ChunkEntry getChunkEntry(long offset) {
        return chunks.get(offset & -Compressor.BLOCK_SIZE_BYTES);
    }

    @NotNull
    public NavigableMap<Long, ChunkEntry> getChunkEntries(@NotNull Span span) {
        final NavigableMap<Long, ChunkEntry> map = chunks.subMap(
            span.offset() & -Compressor.BLOCK_SIZE_BYTES, true,
            span.offset() + span.size() & -Compressor.BLOCK_SIZE_BYTES, true
        );

        if (map.isEmpty()) {
            throw new IllegalArgumentException(String.format("Can't find any chunk entries for span starting at %#x (size: %#x)", span.offset(), span.size()));
        }

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

        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }

        if (normalizeExtension) {
            // If no extension is present, then index would become 0, yielding the same string
            final String extension = path.substring(path.lastIndexOf('.') + 1);

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

    private static void swizzle(@NotNull ByteBuffer buffer, int key1, int key2) {
        final long[] hash = new long[4];
        final byte[] data = IOUtils.toByteArray(
            key1, HEADER_KEY[1], HEADER_KEY[2], HEADER_KEY[3],
            key2, HEADER_KEY[1], HEADER_KEY[2], HEADER_KEY[3]
        );

        System.arraycopy(MurmurHash3.mmh3(data, 0, 16), 0, hash, 0, 2);
        System.arraycopy(MurmurHash3.mmh3(data, 16, 16), 0, hash, 2, 2);

        final ByteBuffer slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < 4; i++) {
            slice.putLong(i * 8, slice.getLong(i * 8) ^ hash[i]);
        }
    }

    public record Header(int magic, int key, long fileSize, long dataSize, long fileEntryCount, int chunkEntryCount, int chunkEntrySize) {
        public static final int BYTES = 40;

        @NotNull
        public static Header read(@NotNull ByteBuffer buffer) {
            assert buffer.remaining() >= BYTES;

            final var magic = buffer.getInt();
            final var key = buffer.getInt();

            assert magic == MAGIC_PLAIN || magic == MAGIC_ENCRYPTED;

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

        public static void swizzle(@NotNull ByteBuffer buffer, @NotNull Span decompressed) {
            final byte[] key = new byte[16];
            IOUtils.put(key, 0, decompressed.offset);
            IOUtils.put(key, 8, decompressed.size);
            IOUtils.put(key, 12, decompressed.key);

            final long[] hash = MurmurHash3.mmh3(key);
            IOUtils.put(key, 0, hash[0]);
            IOUtils.put(key, 8, hash[1]);

            for (int i = 0; i < 4; i++) {
                key[i * 4] ^= DATA_KEY[i] & 0xff;
                key[i * 4 + 1] ^= DATA_KEY[i] >> 8 & 0xff;
                key[i * 4 + 2] ^= DATA_KEY[i] >> 16 & 0xff;
                key[i * 4 + 3] ^= DATA_KEY[i] >> 24 & 0xff;
            }

            System.arraycopy(MD5.get().digest(key), 0, key, 0, 16);

            final ByteBuffer slice = buffer.slice();

            for (int i = 0; i < slice.remaining(); i++) {
                buffer.put(i, (byte) (buffer.get(i) ^ key[i & 15]));
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

        @Override
        public int compareTo(@NotNull Span other) {
            return Long.compare(offset, other.offset);
        }
    }
}
