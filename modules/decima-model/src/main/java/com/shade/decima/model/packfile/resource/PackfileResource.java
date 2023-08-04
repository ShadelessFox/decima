package com.shade.decima.model.packfile.resource;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileBase;
import com.shade.util.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PackfileResource implements Resource {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final Packfile packfile;
    private final PackfileBase.FileEntry entry;
    private final byte[] buffer;

    private InputStream stream;

    public PackfileResource(@NotNull Packfile packfile, @NotNull PackfileBase.FileEntry entry) {
        this.packfile = packfile;
        this.entry = entry;
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
    }

    @Override
    public long read(@NotNull ByteBuffer dst) throws IOException {
        if (stream == null) {
            stream = packfile.newInputStream(entry.hash());
        }

        final int length = Math.min(buffer.length, dst.remaining());
        final int read = stream.read(buffer, 0, length);

        if (read > 0) {
            dst.put(buffer, 0, read);
        }

        return read;
    }

    @Override
    public long hash() {
        return entry.hash();
    }

    @Override
    public int size() {
        return entry.span().size();
    }

    @Override
    public void close() {
        stream = null;
    }
}
