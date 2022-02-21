package com.shade.decima.model.archive.impl;

import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.archive.ArchiveResource;
import com.shade.decima.model.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileResource implements ArchiveResource {
    private final Path path;
    private final FileChannel channel;

    public FileResource(@NotNull Path path) throws IOException {
        this.path = path;
        this.channel = FileChannel.open(path, StandardOpenOption.READ);

        if (channel.size() > Integer.MAX_VALUE) {
            throw new IOException("File is too big");
        }
    }

    @Override
    public int read(@NotNull ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    @Override
    public int size() throws IOException {
        return (int) channel.size();
    }

    @Override
    public long hash() {
        return ArchiveManager.hashFileName(path.toString());
    }

    @NotNull
    @Override
    public String path() {
        return path.toString();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
