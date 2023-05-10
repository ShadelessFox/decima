package com.shade.decima.model.packfile.resource;

import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileResource implements Resource {
    private final FileChannel channel;
    private final int size;
    private final long hash;

    public FileResource(@NotNull Path path, long hash) throws IOException {
        this.channel = FileChannel.open(path, StandardOpenOption.READ);

        final long size = channel.size();

        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File is too big: " + size);
        }

        this.size = (int) size;
        this.hash = hash;
    }

    @Override
    public long read(@NotNull ByteBuffer buffer) throws IOException {
        return channel.read(buffer);
    }

    @Override
    public long hash() {
        return hash;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
