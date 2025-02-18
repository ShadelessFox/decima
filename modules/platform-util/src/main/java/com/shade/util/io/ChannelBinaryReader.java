package com.shade.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

final class ChannelBinaryReader implements BinaryReader {
    private final ByteBuffer buffer = ByteBuffer.allocate(16384)
        .order(ByteOrder.LITTLE_ENDIAN)
        .limit(0);

    private final SeekableByteChannel channel;
    private final long length;
    private long position;

    ChannelBinaryReader(SeekableByteChannel channel) throws IOException {
        this.channel = channel;
        this.length = channel.size();
        this.position = channel.position();
    }

    @Override
    public byte readByte() throws IOException {
        refill(Byte.BYTES);
        return buffer.get();
    }

    @Override
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
    }

    @Override
    public void readBytes(byte[] dst, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, dst.length);

        int remaining = buffer.remaining();

        // If we can read the entire array in one go, do so
        if (remaining >= len) {
            buffer.get(dst, off, len);
            return;
        }

        if (remaining > 0) {
            buffer.get(dst, off, remaining);
            off += remaining;
            len -= remaining;
        }

        // If we can fit the remaining bytes in the buffer, do a refill and read
        if (buffer.capacity() >= len) {
            refill();
            buffer.get(dst, off, len);
            return;
        }

        // If we can't fit the remaining bytes in the buffer, read directly into the destination
        long end = position + buffer.position() + len;
        if (end > length) {
            throw new EOFException();
        }

        read(ByteBuffer.wrap(dst, off, len));
        position = end;
        buffer.limit(0);
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
        Objects.checkIndex(pos, length + 1);

        if (pos >= position && pos < position + buffer.limit()) {
            buffer.position(Math.toIntExact(pos - position));
        } else {
            position = pos;
            buffer.limit(0);
            channel.position(pos);
        }
    }

    @Override
    public ByteOrder order() {
        return buffer.order();
    }

    @Override
    public void order(ByteOrder order) {
        buffer.order(order);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public String toString() {
        return "ChannelBinaryReader[position=" + position() + ", size=" + size() + "]";
    }

    /**
     * Optionally refills the buffer if it contains less than {@code count} bytes remaining.
     *
     * @param count number of bytes to refill
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Compacts the buffer and fills the remaining, updating the {@code position} accordingly.
     *
     * @throws IOException if an I/O error occurs
     */
    private void refill() throws IOException {
        long start = buffer.position() + position;
        long end = Math.min(start + buffer.capacity(), length);

        position = start;
        buffer.compact();
        buffer.limit(Math.toIntExact(end - start));
        read(buffer);
        buffer.flip();
    }

    /**
     * Reads from the channel into the destination buffer until it is full.
     *
     * @param dst destination buffer
     * @throws IOException if an I/O error occurs
     */
    private void read(ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            if (channel.read(dst) < 0) {
                throw new EOFException();
            }
        }
    }
}
