package com.shade.decima.model.archive.dsar;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.READ;

final class DirectStorageArchive implements Archive, Comparable<DirectStorageArchive> {
    private static final int HEADER_SIZE = 32;
    private static final int CHUNK_SIZE = 32;
    private static final int MAGIC = 'D' | 'S' << 8 | 'A' << 16 | 'R' << 24;

    private final HZDRArchiveManager manager;
    private final Path path;
    private final String name;
    private final SeekableByteChannel channel;
    private final List<Chunk> chunks;
    private final Map<Long, File> files;
    private final long size;

    DirectStorageArchive(
        @NotNull HZDRArchiveManager manager,
        @NotNull Path path,
        @NotNull String name,
        @NotNull List<HZDRArchiveManager.FileInfo> fileInfos
    ) throws IOException {
        this.manager = manager;
        this.path = path;
        this.name = name;
        this.channel = Files.newByteChannel(path, READ);

        try {
            final Header header = readHeader();
            this.size = header.totalSize();
            this.chunks = readChunks(header);
            this.files = readFiles(fileInfos);
        } catch (Throwable e) {
            try {
                channel.close();
            } catch (Throwable suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    @NotNull
    @Override
    public HZDRArchiveManager getManager() {
        return manager;
    }

    @NotNull
    @Override
    public String getId() {
        return name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Path getPath() {
        return path;
    }

    @NotNull
    @Override
    public Collection<File> getFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    @Nullable
    @Override
    public File findFile(@NotNull String identifier) {
        return findFile(HZDRArchiveManager.hashPath(identifier));
    }

    @Nullable
    @Override
    public File findFile(long identifier) {
        return files.get(identifier);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int compareTo(@NotNull DirectStorageArchive other) {
        final int result = name.compareToIgnoreCase(other.name);
        return result != 0 ? result : path.compareTo(other.path);
    }

    @NotNull
    private Header readHeader() throws IOException {
        final ByteBuffer buffer = read(0, HEADER_SIZE);
        final int magic = buffer.getInt();
        final int major = Short.toUnsignedInt(buffer.getShort());
        final int minor = Short.toUnsignedInt(buffer.getShort());
        final int chunkCount = buffer.getInt();
        final long firstChunkOffset = Integer.toUnsignedLong(buffer.getInt());
        final long totalSize = buffer.getLong();

        if (magic != MAGIC) {
            throw new IOException("Invalid DirectStorage archive magic in '%s': %08x".formatted(path, magic));
        }
        if (major != 3 || minor != 1) {
            throw new IOException("Unsupported DirectStorage archive version in '%s': %d.%d".formatted(path, major, minor));
        }
        if (chunkCount < 0 || totalSize < 0) {
            throw new IOException("Invalid DirectStorage archive header in '%s'".formatted(path));
        }

        final long tableEnd;
        try {
            tableEnd = Math.addExact(HEADER_SIZE, Math.multiplyExact((long) chunkCount, CHUNK_SIZE));
        } catch (ArithmeticException e) {
            throw new IOException("DirectStorage chunk table is too large in '%s'".formatted(path), e);
        }
        if (tableEnd > channel.size() || firstChunkOffset < tableEnd || firstChunkOffset > channel.size()) {
            throw new IOException("Invalid DirectStorage chunk table in '%s'".formatted(path));
        }

        return new Header(chunkCount, firstChunkOffset, totalSize);
    }

    @NotNull
    private List<Chunk> readChunks(@NotNull Header header) throws IOException {
        final ByteBuffer buffer = read(HEADER_SIZE, Math.multiplyExact(header.chunkCount(), CHUNK_SIZE));
        final List<Chunk> result = new ArrayList<>(header.chunkCount());

        for (int i = 0; i < header.chunkCount(); i++) {
            final long offset = buffer.getLong();
            final long compressedOffset = buffer.getLong();
            final long size = Integer.toUnsignedLong(buffer.getInt());
            final long compressedSize = Integer.toUnsignedLong(buffer.getInt());
            final int type = Byte.toUnsignedInt(buffer.get());
            buffer.position(buffer.position() + 7);

            if (offset < 0 || compressedOffset < 0 || size > Integer.MAX_VALUE || compressedSize > Integer.MAX_VALUE) {
                throw new IOException("Unsupported DirectStorage chunk range in '%s' at index %d".formatted(path, i));
            }
            if (type != 3) {
                throw new IOException("Unsupported DirectStorage compression type in '%s' at index %d: %d".formatted(path, i, type));
            }

            result.add(new Chunk(offset, compressedOffset, (int) size, (int) compressedSize));
        }

        result.sort(Comparator.comparingLong(Chunk::offset));
        long expectedOffset = 0;
        for (int i = 0; i < result.size(); i++) {
            final Chunk chunk = result.get(i);
            final long chunkEnd = checkedEnd(chunk.offset(), chunk.size(), "logical chunk", i);
            final long compressedEnd = checkedEnd(chunk.compressedOffset(), chunk.compressedSize(), "compressed chunk", i);

            if (chunk.offset() != expectedOffset || chunkEnd > header.totalSize()) {
                throw new IOException("Non-contiguous DirectStorage chunks in '%s' at index %d".formatted(path, i));
            }
            if (chunk.compressedOffset() < header.firstChunkOffset() || compressedEnd > channel.size()) {
                throw new IOException("DirectStorage chunk outside archive bounds in '%s' at index %d".formatted(path, i));
            }
            expectedOffset = chunkEnd;
        }
        if (expectedOffset != header.totalSize()) {
            throw new IOException("DirectStorage chunks don't cover the declared size in '%s'".formatted(path));
        }

        return List.copyOf(result);
    }

    @NotNull
    private Map<Long, File> readFiles(@NotNull List<HZDRArchiveManager.FileInfo> infos) throws IOException {
        final Map<Long, File> result = new LinkedHashMap<>(infos.size());

        for (HZDRArchiveManager.FileInfo info : infos) {
            final long end;
            try {
                end = Math.addExact(info.offset(), info.length());
            } catch (ArithmeticException e) {
                throw new IOException("File range overflow in '%s' for %#018x".formatted(path, info.hash()), e);
            }
            if (info.offset() < 0 || info.length() < 0 || end > size) {
                throw new IOException("File outside DirectStorage archive bounds in '%s' for %#018x".formatted(path, info.hash()));
            }
            if (result.putIfAbsent(info.hash(), new File(info.hash(), info.offset(), info.length())) != null) {
                throw new IOException("Duplicate file identifier in '%s': %#018x".formatted(path, info.hash()));
            }
        }

        return result;
    }

    private long checkedEnd(long offset, int length, @NotNull String kind, int index) throws IOException {
        try {
            return Math.addExact(offset, length);
        } catch (ArithmeticException e) {
            throw new IOException("Invalid %s range in '%s' at index %d".formatted(kind, path, index), e);
        }
    }

    @NotNull
    private ByteBuffer read(long offset, int length) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);

        synchronized (channel) {
            channel.position(offset);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    throw new EOFException("Unexpected end of DirectStorage archive '%s'".formatted(path));
                }
            }
        }

        return buffer.flip();
    }

    private int findChunk(long position) throws IOException {
        int low = 0;
        int high = chunks.size() - 1;

        while (low <= high) {
            final int middle = (low + high) >>> 1;
            final Chunk chunk = chunks.get(middle);
            if (position < chunk.offset()) {
                high = middle - 1;
            } else if (position >= chunk.offset() + chunk.size()) {
                low = middle + 1;
            } else {
                return middle;
            }
        }

        throw new IOException("Can't locate DirectStorage chunk for offset %#x in '%s'".formatted(position, path));
    }

    private static void decompressLZ4(@NotNull byte[] source, @NotNull byte[] target) throws IOException {
        int sourcePosition = 0;
        int targetPosition = 0;

        while (sourcePosition < source.length) {
            final int token = Byte.toUnsignedInt(source[sourcePosition++]);
            int literalLength = token >>> 4;

            if (literalLength == 15) {
                int value;
                do {
                    if (sourcePosition >= source.length) {
                        throw new IOException("Truncated LZ4 literal length");
                    }
                    value = Byte.toUnsignedInt(source[sourcePosition++]);
                    literalLength = Math.addExact(literalLength, value);
                } while (value == 255);
            }
            if (literalLength > source.length - sourcePosition || literalLength > target.length - targetPosition) {
                throw new IOException("Invalid LZ4 literal range");
            }

            System.arraycopy(source, sourcePosition, target, targetPosition, literalLength);
            sourcePosition += literalLength;
            targetPosition += literalLength;

            if (sourcePosition == source.length) {
                break;
            }
            if (source.length - sourcePosition < 2) {
                throw new IOException("Truncated LZ4 match offset");
            }

            final int offset = Byte.toUnsignedInt(source[sourcePosition]) | Byte.toUnsignedInt(source[sourcePosition + 1]) << 8;
            sourcePosition += 2;
            int matchLength = token & 0xf;

            if (matchLength == 15) {
                int value;
                do {
                    if (sourcePosition >= source.length) {
                        throw new IOException("Truncated LZ4 match length");
                    }
                    value = Byte.toUnsignedInt(source[sourcePosition++]);
                    matchLength = Math.addExact(matchLength, value);
                } while (value == 255);
            }
            matchLength = Math.addExact(matchLength, 4);

            if (offset == 0 || offset > targetPosition || matchLength > target.length - targetPosition) {
                throw new IOException("Invalid LZ4 match range");
            }
            for (int i = 0; i < matchLength; i++) {
                target[targetPosition + i] = target[targetPosition - offset + i];
            }
            targetPosition += matchLength;
        }

        if (targetPosition != target.length) {
            throw new IOException("LZ4 output size mismatch: expected %d, got %d".formatted(target.length, targetPosition));
        }
    }

    private record Header(int chunkCount, long firstChunkOffset, long totalSize) {}

    private record Chunk(long offset, long compressedOffset, int size, int compressedSize) {}

    final class File implements ArchiveFile {
        private final long identifier;
        private final long offset;
        private final long length;

        private File(long identifier, long offset, long length) {
            this.identifier = identifier;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public long getIdentifier() {
            return identifier;
        }

        @NotNull
        @Override
        public DirectStorageArchive getArchive() {
            return DirectStorageArchive.this;
        }

        @Override
        public long getLength() {
            return length;
        }

        @NotNull
        @Override
        public InputStream newInputStream() {
            return new DirectStorageInputStream(offset, length);
        }
    }

    private final class DirectStorageInputStream extends InputStream {
        private final long end;
        private long position;
        private int chunkIndex = -1;
        private byte[] chunkData;

        private DirectStorageInputStream(long offset, long length) {
            this.position = offset;
            this.end = offset + length;
        }

        @Override
        public int read() throws IOException {
            final byte[] value = new byte[1];
            return read(value, 0, 1) < 0 ? -1 : Byte.toUnsignedInt(value[0]);
        }

        @Override
        public int read(@NotNull byte[] buffer, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, buffer.length);
            if (length == 0) {
                return 0;
            }
            if (position >= end) {
                return -1;
            }

            int total = 0;
            while (length > 0 && position < end) {
                final int index = findChunk(position);
                if (index != chunkIndex) {
                    loadChunk(index);
                }

                final Chunk chunk = chunks.get(index);
                final int chunkOffset = Math.toIntExact(position - chunk.offset());
                final int count = (int) Math.min(Math.min(length, chunk.size() - chunkOffset), end - position);
                System.arraycopy(chunkData, chunkOffset, buffer, offset, count);

                position += count;
                offset += count;
                length -= count;
                total += count;
            }

            return total;
        }

        @Override
        public long skip(long count) {
            if (count <= 0) {
                return 0;
            }
            final long skipped = Math.min(count, end - position);
            position += skipped;
            return skipped;
        }

        @Override
        public int available() {
            return (int) Math.min(end - position, Integer.MAX_VALUE);
        }

        private void loadChunk(int index) throws IOException {
            final Chunk chunk = chunks.get(index);
            final byte[] compressed = DirectStorageArchive.this.read(chunk.compressedOffset(), chunk.compressedSize()).array();
            final byte[] decompressed = new byte[chunk.size()];

            try {
                decompressLZ4(compressed, decompressed);
            } catch (ArithmeticException e) {
                throw new IOException("Invalid LZ4 length in chunk %d of '%s'".formatted(index, path), e);
            } catch (IOException e) {
                throw new IOException("Error decompressing chunk %d of '%s'".formatted(index, path), e);
            }

            chunkIndex = index;
            chunkData = decompressed;
        }
    }
}
