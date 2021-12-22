package com.shade.decima.archive;

import com.shade.decima.Compressor;
import com.shade.decima.util.IOUtils;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.hash.MurmurHash3;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Archive implements Closeable {
    private static final int[] HEADER_CIPHER_KEY = {0x0FA3A9443, 0x0F41CAB62, 0x0F376811C, 0x0D2A89E3E};
    private static final int[] CHUNK_CIPHER_KEY = {0x06C084A37, 0x07E159D95, 0x03D5AF7E8, 0x018AA7D3F};

    private static final int ARCHIVE_TYPE_NORMAL = 0x20304050;
    private static final int ARCHIVE_TYPE_ENCRYPTED = 0x21304050;

    private final Path path;
    private final FileChannel channel;
    private final int type;
    private final int key;
    private final long fileSize;
    private final long dataSize;
    private final int maximumChunkSize;

    private final List<FileEntry> fileEntries = new ArrayList<>();
    private final List<ChunkEntry> chunkEntries = new ArrayList<>();

    public Archive(@NotNull Path path) throws IOException {
        this.path = path;
        this.channel = FileChannel.open(path);

        final ByteBuffer header = IOUtils.readExact(channel, 40);

        this.type = header.getInt();
        this.key = header.getInt();

        if (type != ARCHIVE_TYPE_NORMAL && type != ARCHIVE_TYPE_ENCRYPTED) {
            throw new IOException("Invalid archive header");
        }

        if (type == ARCHIVE_TYPE_ENCRYPTED) {
            decryptHeader(header, key, key + 1);
        }

        this.fileSize = header.getLong();
        this.dataSize = header.getLong();
        final long fileEntriesCount = header.getLong();
        final int chunkEntriesCount = header.getInt();
        this.maximumChunkSize = header.getInt();

        for (long i = 0; i < fileEntriesCount; i++) {
            final ByteBuffer buffer = IOUtils.readExact(channel, FileEntry.BYTES);

            if (type == ARCHIVE_TYPE_ENCRYPTED) {
                final int key1 = buffer.getInt(4);
                final int key2 = buffer.getInt(28);

                decryptHeader(buffer, key1, key2);

                buffer.putInt(4, key1);
                buffer.putInt(28, key2);
            }

            final FileEntry entry = new FileEntry(buffer);

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

            chunkEntries.add(new ChunkEntry(buffer));
        }

        chunkEntries.sort(Comparator.comparing(x -> x.compressedSpan().offset()));
    }

    public int getKey() {
        return key;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    @NotNull
    public List<FileEntry> getFileEntries() {
        return fileEntries;
    }

    @NotNull
    public List<ChunkEntry> getChunkEntries() {
        return chunkEntries;
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
        final int startChunkIndex = getChunkFromOffset(file.span().offset());
        final int lastChunkIndex = getChunkFromOffset(file.span().offset() + file.span().size());

        for (int i = startChunkIndex; i <= lastChunkIndex; i++) {
            final ChunkEntry chunk = chunkEntries.get(i);

            final ByteBuffer chunkBufferCompressed = ByteBuffer.allocate(chunk.compressedSpan().size());
            channel.position(chunk.compressedSpan().offset());
            channel.read(chunkBufferCompressed.slice());

            if (type == ARCHIVE_TYPE_ENCRYPTED) {
                decryptChunk(chunkBufferCompressed, chunk);
            }

            final ByteBuffer chunkBufferDecompressed = ByteBuffer.allocate(chunk.decompressedSpan().size());
            compressor.decompress(chunkBufferCompressed.array(), chunkBufferDecompressed.array());

            if (i == startChunkIndex) {
                final int offset = (int) (file.span().offset() & (maximumChunkSize - 1));
                final int length = chunkBufferDecompressed.remaining() - offset;
                buffer.put(chunkBufferDecompressed.slice(offset, length));
            } else if (i == lastChunkIndex) {
                final int offset = 0;
                final int length = buffer.remaining();
                buffer.put(chunkBufferDecompressed.slice(offset, length));
            } else {
                buffer.put(chunkBufferDecompressed);
            }
        }

        final byte[] result = new byte[file.span().size()];
        buffer.position(0);
        buffer.get(result, (int) (file.span().offset() % maximumChunkSize), result.length);

        return result;
    }

    private int getChunkFromOffset(long offset) throws IOException {
        final long aligned = offset & -maximumChunkSize;

        for (int i = 0; i < chunkEntries.size(); i++) {
            if (chunkEntries.get(i).decompressedSpan().offset() == aligned) {
                return i;
            }
        }

        throw new IOException(String.format("Can't find chunk entry from offset %#x (aligned: %#x)", offset, aligned));
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

    public static record FileEntry(int index, int key, long hash, Span span) {
        public static final int BYTES = 16 + Span.BYTES;

        public FileEntry(@NotNull ByteBuffer buffer) {
            this(buffer.getInt(), buffer.getInt(), buffer.getLong(), new Span(buffer));
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
