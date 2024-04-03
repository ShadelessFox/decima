package com.shade.decima.model.archive;

import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class ChunkedInputStream extends InputStream {
    protected final byte[] compressed;
    protected final byte[] decompressed;

    protected int count; // count of bytes in the current chunk
    protected int pos; // position in the current chunk

    public ChunkedInputStream(int compressedBufferSize, int decompressedBufferSie) {
        this.compressed = new byte[compressedBufferSize];
        this.decompressed = new byte[decompressedBufferSie];
    }

    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
        }
        if (pos >= count) {
            return -1;
        }
        return decompressed[pos++] & 0xff;
    }

    @Override
    public int read(@NotNull byte[] buf, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, buf.length);

        if (len == 0) {
            return 0;
        }

        for (int n = 0; ; ) {
            final int read = read1(buf, off + n, len - n);
            if (read <= 0) {
                return n == 0 ? read : n;
            }
            n += read;
            if (n >= len) {
                return n;
            }
        }
    }

    private int read1(@NotNull byte[] buf, int off, int len) throws IOException {
        int available = count - pos;

        if (available <= 0) {
            fill();
            available = count - pos;
        }

        if (available <= 0) {
            return -1;
        }

        final int count = Math.min(available, len);
        System.arraycopy(decompressed, pos, buf, off, count);
        pos += count;

        return count;
    }

    protected abstract void fill() throws IOException;
}
