package com.shade.decima.rtti.test;

import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;
import com.shade.util.io.ChunkedBinaryReader;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class CompressedBinaryReader extends ChunkedBinaryReader {
    private static final LZ4FastDecompressor lz4 = LZ4Factory.fastestInstance().fastDecompressor();
    private final Header header;

    private CompressedBinaryReader(@NotNull BinaryReader reader, @NotNull Header header, @NotNull List<Chunk> chunks) {
        super(reader, chunks);
        this.header = header;
    }

    @NotNull
    public static BinaryReader open(@NotNull Path path) throws IOException {
        var reader = BinaryReader.open(path);
        var header = Header.read(reader);
        if (!header.isChunked() || !header.isCompressed() || header.getCompressionMode() != 3) {
            throw new IOException("Unsupported file format");
        }

        var chunkCount = Math.toIntExact(Math.ceilDiv(header.dataSize(), header.chunkSize()));
        var chunkSizes = reader.readInts(chunkCount);

        var actualFileSize = IntStream.of(chunkSizes).sum() + 32 + chunkCount * Integer.BYTES;
        if (actualFileSize != reader.size()) {
            throw new IOException("File size mismatch: expected %d but found %d".formatted(actualFileSize, reader.size()));
        }

        var chunks = new ArrayList<Chunk>(chunkCount);
        var chunkOffset = 0L;
        var chunkCompressedOffset = reader.position();

        for (int chunkCompressedSize : chunkSizes) {
            var chunkSize = Math.min(header.chunkSize(), Math.toIntExact(header.dataSize() - chunkOffset));
            chunks.add(new Chunk(chunkOffset, chunkCompressedOffset, chunkSize, chunkCompressedSize));
            chunkOffset += chunkSize;
            chunkCompressedOffset += chunkCompressedSize;
        }

        return new CompressedBinaryReader(reader, header, chunks);
    }

    @Override
    public long size() {
        return header.dataSize();
    }

    @Override
    protected void decompress(@NotNull byte[] src, @NotNull byte[] dst, int length) {
        lz4.decompress(src, dst, length);
    }

    @Override
    public String toString() {
        return "CompressedBinaryHeader[position=" + position() + ", size=" + size() + "]";
    }

    private record Header(int signature, int chunkSize, long dataSize, byte[] checksum) {
        static Header read(BinaryReader reader) throws IOException {
            var signature = reader.readInt();
            if (signature >>> 8 != 0xCB10C1) {
                throw new IOException("Invalid file signature: expected %06x, but found %06x".formatted(0xCB10C1, signature >>> 8));
            }
            var chunkSize = reader.readInt();
            var dataSize = reader.readLong();
            var checksum = reader.readBytes(16);
            return new Header(signature, chunkSize, dataSize, checksum);
        }

        boolean isChunked() {
            return (signature & 0x80) != 0;
        }

        boolean isCompressed() {
            return (signature & 0x7f) != 0;
        }

        int getCompressionMode() {
            return signature & 0x7f;
        }
    }
}
