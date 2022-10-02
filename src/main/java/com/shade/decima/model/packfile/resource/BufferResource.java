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
    public long read(@NotNull ByteBuffer buffer) throws IOException {
        final int length = Math.min(data.length - position, buffer.remaining());
        buffer.put(data, position, length);
        position += length;
        return length;
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
        // nothing to close
    }
}
