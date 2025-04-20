package com.shade.util.compression;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public abstract sealed class Decompressor implements Closeable permits LZ4Decompressor, NoneDecompressor, OodleDecompressor {
    public static Decompressor none() {
        return new NoneDecompressor();
    }

    public static Decompressor lz4() {
        return new LZ4Decompressor();
    }

    public static Decompressor oodle(Path path) {
        return new OodleDecompressor(path);
    }

    public abstract void decompress(ByteBuffer src, ByteBuffer dst) throws IOException;

    public void decompress(byte[] src, int srcLen, byte[] dst, int dstLen) throws IOException {
        decompress(src, 0, srcLen, dst, 0, dstLen);
    }

    public void decompress(byte[] src, int srcOff, int srcLen, byte[] dst, int dstOff, int dstLen) throws IOException {
        decompress(ByteBuffer.wrap(src, srcOff, srcLen), ByteBuffer.wrap(dst, dstOff, dstLen));
    }

    @Override
    public void close() throws IOException {
        // nothing to close by default
    }
}
