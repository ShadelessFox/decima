package com.shade.decima.model.packfile.resource;

import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferResource implements Resource {
    private final byte[] data;
    private final long hash;
    private int position;

    public BufferResource(@NotNull byte[] data, long hash) {
        this.data = data;
        this.hash = hash;
        this.position = 0;
    }

    @Override
    public int read(@NotNull ByteBuffer buffer) throws IOException {
        if (position < 0) {
            throw new IOException("Resource is closed");
        }

        final int available = Math.min(remaining(), buffer.remaining());

        if (available <= 0) {
            return -1;
        }

        buffer.put(data, position, available);
        position += available;
        return available;
    }

    @Override
    public long hash() {
        return hash;
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    public void close() {
        position = -1;
    }

    private int remaining() {
        return Math.max(data.length - position, 0);
    }
}
