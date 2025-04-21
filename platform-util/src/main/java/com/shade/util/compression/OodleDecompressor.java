package com.shade.util.compression;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

final class OodleDecompressor extends Decompressor {
    private final Path path;

    public OodleDecompressor(Path path) {
        this.path = path;
    }

    @Override
    public void decompress(ByteBuffer src, ByteBuffer dst) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public void close() throws IOException {
        // TODO close
    }
}
