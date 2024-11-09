package com.shade.decima.rtti.test;

import com.shade.util.io.BinaryReader;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public final class CompressedBinaryReader implements BinaryReader {
    private final LZ4FastDecompressor lz4 = LZ4Factory.fastestInstance().fastDecompressor();
    private final BinaryReader reader;
    private final Header header;
    private final int[] chunks; // size of compressed data, in chunks

    private final ByteBuffer buffer;
    private final long offset; // offset where the compressed data starts
    private final long length; // total length of decompressed data

    private long position;

    private CompressedBinaryReader(BinaryReader reader, Header header, int[] chunks) throws IOException {
        this.reader = reader;
        this.header = header;
        this.chunks = chunks;

        this.buffer = ByteBuffer.allocate(header.chunkSize()).order(ByteOrder.LITTLE_ENDIAN).limit(0);
        this.offset = reader.position();
        this.length = header.dataSize();
    }

    public static BinaryReader open(Path path) throws IOException {
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

        return new CompressedBinaryReader(reader, header, chunkSizes);
    }

    @Override
    public byte readByte() throws IOException {
        refill(Byte.BYTES);
        return buffer.get();
    }

    /*@Override
    public short readShort() throws IOException {
        refill(Short.BYTES);
        return buffer.getShort();
    }

    @Override
    public int readInt() throws IOException {
        refill(Integer.BYTES);
        return buffer.getInt();
    }

    @Override
    public long readLong() throws IOException {
        refill(Long.BYTES);
        return buffer.getLong();
    }

    @Override
    public float readFloat() throws IOException {
        refill(Float.BYTES);
        return buffer.getFloat();
    }

    @Override
    public double readDouble() throws IOException {
        refill(Double.BYTES);
        return buffer.getDouble();
    }*/

    @Override
    public void readBytes(byte[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);
        int remaining = buffer.remaining();
        if (remaining > len) {
            buffer.get(dst, off, len);
            return;
        }
        if (remaining > 0) {
            buffer.get(dst, off, remaining);
            off += remaining;
            len -= remaining;
        }
        while (len > 0) {
            refill();
            int length = Math.min(len, buffer.remaining());
            buffer.get(dst, off, length);
            off += length;
            len -= length;
        }
    }

    @Override
    public long size() {
        return length;
    }

    @Override
    public long position() {
        return position + buffer.position();
    }

    @Override
    public void position(long pos) throws IOException {
        Objects.checkIndex(pos, length);

        if (pos >= position && pos < position + buffer.limit()) {
            buffer.position(Math.toIntExact(pos - position));
        } else {
            int offset = Math.toIntExact(pos % header.chunkSize());
            position = pos / header.chunkSize() * header.chunkSize();
            buffer.limit(0);
            refill();
            buffer.position(offset);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public String toString() {
        return "CompressedBinaryHeader[position=" + position() + ", size=" + size() + "]";
    }

    private void refill(int count) throws IOException {
        if (buffer.capacity() < count) {
            throw new IllegalArgumentException("Can't refill more bytes than the buffer can hold");
        }
        if (buffer.remaining() < count) {
            refill();
            if (buffer.remaining() < count) {
                throw new EOFException("Expected to read " + count + " bytes, but only " + buffer.remaining() + " bytes are available");
            }
        }
    }

    private void refill() throws IOException {
        if (buffer.hasRemaining()) {
            throw new IllegalStateException("Buffer is not empty");
        }

        long start = buffer.position() + position;
        long end = Math.min(start + buffer.capacity(), length);

        var chunkIndex = Math.toIntExact(Math.ceilDiv(start, header.chunkSize()));
        var chunkStart = Arrays.stream(chunks, 0, chunkIndex).sum() + offset;
        var chunkData = new byte[chunks[chunkIndex]];

        reader.position(chunkStart);
        reader.readBytes(chunkData, 0, chunkData.length);

        position = start;
        buffer.compact();
        buffer.limit(Math.toIntExact(end - start));
        lz4.decompress(chunkData, 0, buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
    }

    private record Header(int signature, int chunkSize, long dataSize, byte[] checksum) {
        static Header read(BinaryReader reader) throws IOException {
            var signature = reader.readInt();
            if (signature >>> 8 != 0xCB10C1) {
                throw new IOException("Invalid file signature: expected %06x, but found %06x".formatted(0xCB10C1, signature >>> 8));
            }
            var blockSize = reader.readInt();
            var dataSize = reader.readLong();
            var checksum = reader.readBytes(16);
            return new Header(signature, blockSize, dataSize, checksum);
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
