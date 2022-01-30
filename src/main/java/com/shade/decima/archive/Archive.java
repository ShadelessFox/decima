package com.shade.decima.archive;

import com.shade.decima.util.Compressor;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Archive implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Archive.class);

    private static final int[] HEADER_CIPHER_KEY = {0x0FA3A9443, 0x0F41CAB62, 0x0F376811C, 0x0D2A89E3E};
    private static final int[] CHUNK_CIPHER_KEY = {0x06C084A37, 0x07E159D95, 0x03D5AF7E8, 0x018AA7D3F};

    private static final int ARCHIVE_TYPE_NORMAL = 0x20304050;
    private static final int ARCHIVE_TYPE_ENCRYPTED = 0x21304050;

    private final Path path;
    private final String name;
    private final FileChannel channel;
    private final int type;
    private final int maximumChunkSize;

    private final List<FileEntry> fileEntries = new ArrayList<>();
    private final NavigableMap<Long, ChunkEntry> chunkEntries = new TreeMap<>();

    public Archive(@NotNull Path path, @NotNull String name) throws IOException {
        log.debug("Loading archive '{}'...", path.getFileName());

        this.path = path;
        this.channel = FileChannel.open(path);
        this.name = name;

        final ByteBuffer header = IOUtils.readExact(channel, 40);

        this.type = header.getInt();
        int key = header.getInt();

        if (type != ARCHIVE_TYPE_NORMAL && type != ARCHIVE_TYPE_ENCRYPTED) {
            throw new IOException("Invalid archive header");
        }

        if (type == ARCHIVE_TYPE_ENCRYPTED) {
            decryptHeader(header, key, key + 1);
        }

        long fileSize = header.getLong();
        long dataSize = header.getLong();
        final long fileEntriesCount = header.getLong();
        final int chunkEntriesCount = header.getInt();
        this.maximumChunkSize = header.getInt();

        log.debug("Archive has {} file(-s), {} byte(-s) in total", fileEntriesCount, fileSize);

        for (long i = 0; i < fileEntriesCount; i++) {
            final ByteBuffer buffer = IOUtils.readExact(channel, FileEntry.BYTES);

            if (type == ARCHIVE_TYPE_ENCRYPTED) {
                final int key1 = buffer.getInt(4);
                final int key2 = buffer.getInt(28);

                decryptHeader(buffer, key1, key2);

                buffer.putInt(4, key1);
                buffer.putInt(28, key2);
            }

            final FileEntry entry = new FileEntry(this, buffer);

            fileEntries.add(entry);
        }

        for (long i = 0; i < chunkEntriesCount; i++) {
            final ByteBuffer buffer = IOUtils.readExact(channel, ChunkEntry.BYTES);

            if (type == ARCHIVE_TYPE_ENCRYPTED) {
                final int key1 = buffer.getInt(12);
                final int key2 = buffer.getInt(28);

                decryptHeader(buffer, key1, key2);

                buffer.putInt(12, key1);
                buffer.putInt(28, key2);
            }

            final ChunkEntry entry = new ChunkEntry(buffer);

            chunkEntries.put(entry.decompressedSpan().offset(), entry);
        }
    }

    @NotNull
    public List<FileEntry> getFileEntries() {
        return fileEntries;
    }

    private void decryptHeader(@NotNull ByteBuffer buf, int key1, int key2) {
        decryptHeader(buf.slice().order(ByteOrder.LITTLE_ENDIAN), buf.slice().order(ByteOrder.LITTLE_ENDIAN), key1, key2);
    }

    private void decryptHeader(@NotNull ByteBuffer src, @NotNull ByteBuffer dst, int key1, int key2) {
        final long[] iv = new long[4];
        final byte[] key = IOUtils.toByteArray(new int[]{
            key1, HEADER_CIPHER_KEY[1], HEADER_CIPHER_KEY[2], HEADER_CIPHER_KEY[3],
            key2, HEADER_CIPHER_KEY[1], HEADER_CIPHER_KEY[2], HEADER_CIPHER_KEY[3]
        });

        System.arraycopy(MurmurHash3.mmh3(key, 0, 16), 0, iv, 0, 2);
        System.arraycopy(MurmurHash3.mmh3(key, 16, 16), 0, iv, 2, 2);

        for (int i = 0; i < 4; i++) {
            dst.putLong(src.getLong() ^ iv[i]);
        }
    }

    private void decryptChunk(@NotNull ByteBuffer buf, @NotNull ChunkEntry chunkEntry) {
        decryptChunk(buf.slice().order(ByteOrder.LITTLE_ENDIAN), buf.slice().order(ByteOrder.LITTLE_ENDIAN), chunkEntry);
    }

    private void decryptChunk(@NotNull ByteBuffer src, @NotNull ByteBuffer dst, @NotNull ChunkEntry chunkEntry) {
        final ByteBuffer key = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        chunkEntry.decompressedSpan().write(key);

        final byte[] iv = IOUtils.toByteArray(MurmurHash3.mmh3(key.array(), 0, 16));

        for (int i = 0; i < 4; i++) {
            iv[i * 4] ^= CHUNK_CIPHER_KEY[i] & 0xff;
            iv[i * 4 + 1] ^= CHUNK_CIPHER_KEY[i] >> 8 & 0xff;
            iv[i * 4 + 2] ^= CHUNK_CIPHER_KEY[i] >> 16 & 0xff;
            iv[i * 4 + 3] ^= CHUNK_CIPHER_KEY[i] >> 24 & 0xff;
        }

        final byte[] digest;

        try {
            digest = MessageDigest.getInstance("MD5").digest(iv);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < chunkEntry.compressedSpan().size(); i++) {
            dst.put((byte) (src.get() ^ digest[i & 15]));
        }
    }

    @NotNull
    public byte[] unpack(@NotNull Compressor compressor, @NotNull FileEntry file) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(file.span().size());
        final NavigableMap<Long, ChunkEntry> chunks = getChunks(file.span());

        for (Map.Entry<Long, ChunkEntry> entry : chunks.entrySet()) {
            final ChunkEntry chunk = entry.getValue();

            final ByteBuffer chunkBufferCompressed = ByteBuffer.allocate(chunk.compressedSpan().size());
            channel.position(chunk.compressedSpan().offset());
            channel.read(chunkBufferCompressed.slice());

            if (type == ARCHIVE_TYPE_ENCRYPTED) {
                decryptChunk(chunkBufferCompressed, chunk);
            }

            final ByteBuffer chunkBufferDecompressed = ByteBuffer.allocate(chunk.decompressedSpan().size());
            compressor.decompress(chunkBufferCompressed.array(), chunkBufferDecompressed.array());

            if (entry.equals(chunks.firstEntry())) {
                final int offset = (int) (file.span().offset() & (maximumChunkSize - 1));
                final int length = Math.min(buffer.remaining(), chunkBufferDecompressed.remaining() - offset);
                buffer.put(chunkBufferDecompressed.slice(offset, length));
            } else if (entry.equals(chunks.lastEntry())) {
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
    private NavigableMap<Long, ChunkEntry> getChunks(@NotNull Span span) throws IOException {
        final NavigableMap<Long, ChunkEntry> map = chunkEntries.subMap(
            span.offset() & -maximumChunkSize, true,
            span.offset() + span.size() & -maximumChunkSize, true
        );

        if (map.isEmpty()) {
            throw new IOException(String.format("Can't find any chunk entries for span starting at %#x (size: %#x)", span.offset(), span.size()));
        }

        return map;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public String toString() {
        return "Archive{" + "path=" + path + '}';
    }

    public static record ChunkEntry(Span decompressedSpan, Span compressedSpan) {
        public static final int BYTES = Span.BYTES * 2;

        public ChunkEntry(@NotNull ByteBuffer buffer) {
            this(new Span(buffer), new Span(buffer));
        }
    }

    public static record FileEntry(@NotNull Archive archive, int index, int key, long hash, Span span) {
        public static final int BYTES = 16 + Span.BYTES;

        public FileEntry(@NotNull Archive archive, @NotNull ByteBuffer buffer) {
            this(archive, buffer.getInt(), buffer.getInt(), buffer.getLong(), new Span(buffer));
        }
    }

    public static record Span(long offset, int size, int key) {
        public static final int BYTES = 16;

        public Span(@NotNull ByteBuffer buffer) {
            this(buffer.getLong(), buffer.getInt(), buffer.getInt());
        }

        public void write(@NotNull ByteBuffer buffer) {
            buffer.putLong(offset);
            buffer.putInt(size);
            buffer.putInt(key);
        }
    }
}
