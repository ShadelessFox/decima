package com.shade.decima.model.packfile.resource;

import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface Resource extends Closeable {
    long read(@NotNull ByteBuffer buffer) throws IOException;

    long hash();

    int size();
}
