package com.shade.decima.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.file.Path;

public class Compressor {
    public static final int BLOCK_SIZE_BYTES = 0x40000;

    private final OodleLibrary library;
    private final Path path;

    public Compressor(@NotNull Path path) {
        this.library = Native.load(path.toString(), OodleLibrary.class);
        this.path = path;
    }

    public int compress(@NotNull byte[] src, @NotNull byte[] dst) throws IOException {
        final int size = library.OodleLZ_Compress(8, src, src.length, dst, 4, 0, 0, 0, 0, 0);
        if (size == 0) {
            throw new IOException("Error compressing data");
        }
        return size;
    }

    public void decompress(@NotNull byte[] src, @NotNull byte[] dst) throws IOException {
        final int decompressed = library.OodleLZ_Decompress(src, src.length, dst, dst.length, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        if (decompressed != dst.length) {
            throw new IOException("Error decompressing buffer");
        }
    }

    public int getVersion() {
        final int[] buffer = new int[7];
        library.Oodle_GetConfigValues(buffer);
        return buffer[6];
    }

    @NotNull
    public String getVersionString() {
        final int version = getVersion();
        return String.format("%d.%d.%d", (version & 0xff) - (version >>> 24), version >>> 16 & 0xff, version >>> 8 & 0xff);
    }

    public static int getCompressedSize(int size) {
        return size + 274 * getBlocksCount(size);
    }

    public static int getBlocksCount(int size) {
        return (size + BLOCK_SIZE_BYTES - 1) / BLOCK_SIZE_BYTES;
    }

    @Override
    public String toString() {
        return "Compressor{path=" + path + ", version=" + getVersionString() + '}';
    }

    private interface OodleLibrary extends Library {
        int OodleLZ_Compress(int compressor, byte[] rawBuf, long rawLen, byte[] compBuf, int level, long pOptions, long dictionaryBase, long lrm, long scratchMem, long scratchSize);

        int OodleLZ_Decompress(byte[] compBuf, long compBufSize, byte[] rawBuf, long rawLen, int fuzzSafe, int checkCRC, int verbosity, long decBufBase, long decBufSize, long fpCallback, long callbackUserData, long decoderMemory, long decoderMemorySize, int threadPhase);

        void Oodle_GetConfigValues(int[] buffer);
    }
}
