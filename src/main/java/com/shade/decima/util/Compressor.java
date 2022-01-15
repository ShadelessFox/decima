package com.shade.decima.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.file.Path;

public class Compressor {
    private final OodleLibrary library;
    private final Path path;

    public Compressor(@NotNull Path path) {
        this.library = Native.load(path.toString(), OodleLibrary.class);
        this.path = path;
    }

    public int compress(@NotNull byte[] src, @NotNull byte[] dst) {
        return library.OodleLZ_Compress(8, src, src.length, dst, 4, 0, 0, 0, 0, 0);
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
        return size + 274 * ((size + 0x3FFFF) / 0x40000);
    }

    @Override
    public String toString() {
        return "Compressor{path=" + path + ", version=" + getVersionString() + '}';
    }

    private interface OodleLibrary extends Library {
        int OodleLZ_Compress(int format, byte[] src, long srcLen, byte[] dst, int level, long unk1, long unk2, long unk3, long unk4, long unk5);

        int OodleLZ_Decompress(byte[] src, long srcLen, byte[] dst, long dstLen, int fuzz, int crc, int verbose, long unk1, long unk2, long unk3, long unk4, long unk5, long unk6, int unk7);

        void Oodle_GetConfigValues(int[] buffer);
    }
}
