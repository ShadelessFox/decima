package com.shade.decima.archive;

import com.shade.decima.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface ArchiveResource extends Closeable {
    int read(@NotNull ByteBuffer dst) throws IOException;

    int size() throws IOException;

    long hash();

    @NotNull
    String path();
}
