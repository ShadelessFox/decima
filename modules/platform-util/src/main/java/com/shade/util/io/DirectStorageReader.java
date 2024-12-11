package com.shade.util.io;

import com.shade.util.NotNull;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * A reader for DirectStorage archive.
 *
 * @see <a href="https://github.com/ShadelessFox/decima/wiki/Archives#directstorage-archive">DirectStorage archive format</a>
 */
public class DirectStorageReader extends ChunkedBinaryReader {
    private static final LZ4FastDecompressor lz4 = LZ4Factory.fastestInstance().fastDecompressor();
    private final Header header;

    private DirectStorageReader(@NotNull BinaryReader reader, @NotNull Header header, @NotNull List<Chunk> chunks) {
        super(reader, chunks);
        this.header = header;
    }

    @NotNull
    public static BinaryReader open(@NotNull Path path) throws IOException {
        var reader = BinaryReader.open(path);
        var header = Header.read(reader);
        var chunks = reader.readObjects(header.chunkCount(), DirectStorageReader::readChunk);

        return new DirectStorageReader(reader, header, chunks);
    }

    @Override
    public long size() {
        return header.totalSize;
    }

    @Override
    protected void decompress(@NotNull byte[] src, @NotNull byte[] dst, int length) {
        lz4.decompress(src, dst, length);
    }

    @Override
    public String toString() {
        return "DirectStorageReader[position=" + position() + ", size=" + size() + "]";
    }

    @NotNull
    private static Chunk readChunk(@NotNull BinaryReader reader) throws IOException {
        var offset = reader.readLong();
        var compressedOffset = reader.readLong();
        var size = reader.readInt();
        var compressedSize = reader.readInt();
        var type = reader.readByte();
        reader.skip(7); // padding

        if (type != 3) { // lz4
            throw new IllegalArgumentException("Unsupported chunk compression type: " + type);
        }

        return new Chunk(offset, compressedOffset, size, compressedSize);
    }

    private record Header(
        int magic,
        short versionMajor,
        short versionMinor,
        int chunkCount,
        int firstChunkOffset,
        long totalSize
    ) {
        static final int MAGIC = 'D' | 'S' << 8 | 'A' << 16 | 'R' << 24;

        @NotNull
        static Header read(@NotNull BinaryReader reader) throws IOException {
            var magic = reader.readInt();
            var versionMajor = reader.readShort();
            var versionMinor = reader.readShort();
            var chunkCount = reader.readInt();
            var firstChunkOffset = reader.readInt();
            var totalSize = reader.readLong();
            reader.skip(8); // padding

            if (magic != MAGIC) {
                throw new IOException("Invalid archive magic: %08x".formatted(magic));
            }

            if (versionMajor != 3 && versionMinor != 1) {
                throw new IOException("Unsupported archive version: %d.%d".formatted(versionMajor, versionMinor));
            }

            return new Header(magic, versionMajor, versionMinor, chunkCount, firstChunkOffset, totalSize);
        }
    }
}
