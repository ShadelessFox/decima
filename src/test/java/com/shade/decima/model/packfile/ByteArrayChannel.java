package com.shade.decima.model.packfile;

import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Adapted from {@link jdk.nio.zipfs.ByteArrayChannel}
 * <p>
 * Used for tests only
 */
class ByteArrayChannel implements SeekableByteChannel {
    private static final byte[] EMPTY_BUFFER = new byte[0];

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private byte[] buf;
    private int pos;
    private int last;
    private boolean closed;

    public ByteArrayChannel(@NotNull byte[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.last = buf.length;
    }

    public ByteArrayChannel() {
        this(EMPTY_BUFFER);
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public long position() throws IOException {
        beginRead();
        try {
            ensureOpen();
            return pos;
        } finally {
            endRead();
        }
    }

    @Override
    public SeekableByteChannel position(long pos) throws IOException {
        beginWrite();
        try {
            ensureOpen();
            if (pos < 0 || pos >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Illegal position " + pos);
            ensureCapacity((int) pos);
            this.pos = Math.min((int) pos, last);
            return this;
        } finally {
            endWrite();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        beginWrite();
        try {
            ensureOpen();
            if (pos == last)
                return -1;
            int n = Math.min(dst.remaining(), last - pos);
            dst.put(buf, pos, n);
            pos += n;
            return n;
        } finally {
            endWrite();
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        ensureOpen();
        if (size < 0 || size >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Illegal size " + size);
        if (size > last)
            return this;
        last = (int) size;
        buf = Arrays.copyOf(buf, last);
        if (pos > last)
            pos = last;
        return this;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        beginWrite();
        try {
            ensureOpen();
            int n = src.remaining();
            ensureCapacity(pos + n);
            src.get(buf, pos, n);
            pos += n;
            if (pos > last) {
                last = pos;
            }
            return n;
        } finally {
            endWrite();
        }
    }

    @Override
    public long size() throws IOException {
        beginRead();
        try {
            ensureOpen();
            return last;
        } finally {
            endRead();
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        beginWrite();
        try {
            closed = true;
            buf = null;
            pos = 0;
            last = 0;
        } finally {
            endWrite();
        }
    }

    private void ensureOpen() throws IOException {
        if (closed)
            throw new ClosedChannelException();
    }

    private void beginWrite() {
        lock.writeLock().lock();
    }

    private void endWrite() {
        lock.writeLock().unlock();
    }

    private void beginRead() {
        lock.readLock().lock();
    }

    private void endRead() {
        lock.readLock().unlock();
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
        last = newCapacity;
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }
}
