package com.shade.decima.model.packfile.resource;

import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface Resource extends Closeable {
    /**
     * Reads a sequence of bytes from this resource into the given buffer.
     *
     * @param buffer The buffer into which bytes are to be transferred
     * @return The number of bytes read, possibly zero, or {@code -1} if the channel has reached end-of-stream
     * @throws IOException If I/O error occurs
     */
    long read(@NotNull ByteBuffer buffer) throws IOException;

    long hash();

    int size();
}
