package com.shade.decima.model.util;

import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import com.sun.jna.InvocationMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Compressor implements Closeable {
    public static final int BLOCK_SIZE_BYTES = 0x40000;

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    private static final Map<String, Object> LIBRARY_OPTIONS = Map.of(
        Library.OPTION_INVOCATION_MAPPER, (InvocationMapper) (lib, m) -> {
            if (m.getName().equals("dispose")) {
                return (proxy, method, args) -> {
                    lib.close();
                    return null;
                };
            }
            return null;
        }
    );

    private static final Map<Path, Reference<Compressor>> compressors = new ConcurrentHashMap<>();

    private final OodleLibrary library;
    private final Path path;
    private volatile int useCount;

    private Compressor(@NotNull Path path) {
        this.library = Native.load(path.toString(), OodleLibrary.class, LIBRARY_OPTIONS);
        this.path = path;
        this.useCount = 1;
    }

    @NotNull
    public static Compressor acquire(@NotNull Path path) {
        final Reference<Compressor> ref = compressors.get(path);
        Compressor compressor = ref != null ? ref.get() : null;

        if (compressor == null) {
            compressor = new Compressor(path);
            compressors.put(path, new WeakReference<>(compressor));
        } else {
            synchronized (compressor.library) {
                compressor.useCount += 1;
            }
        }

        return compressor;
    }

    @NotNull
    public ByteBuffer compress(@NotNull ByteBuffer input, @NotNull Level level) throws IOException {
        final byte[] src = BufferUtils.getBytes(input, input.remaining());
        final byte[] dst = new byte[getCompressedSize(src.length)];
        final int length = compress(src, dst, level);

        return ByteBuffer.wrap(dst, 0, length);
    }

    public int compress(@NotNull byte[] src, @NotNull byte[] dst, @NotNull Level level) throws IOException {
        if (src.length == 0) {
            return 0;
        }
        final int size = library.OodleLZ_Compress(8, src, src.length, dst, level.value, 0, 0, 0, 0, 0);
        if (size == 0) {
            throw new IOException("Error compressing data");
        }
        return size;
    }

    public void decompress(@NotNull byte[] src, @NotNull byte[] dst) throws IOException {
        decompress(src, src.length, dst, dst.length);
    }

    public void decompress(@NotNull byte[] src, int srcLen, @NotNull byte[] dst, int dstLen) throws IOException {
        Objects.checkFromIndexSize(0, srcLen, src.length);
        Objects.checkFromIndexSize(0, dstLen, dst.length);

        if (srcLen == 0) {
            return;
        }

        final int size = library.OodleLZ_Decompress(src, srcLen, dst, dstLen, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        if (size != dstLen) {
            throw new IOException("Error decompressing data");
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
        // The packed data is:
        //     (46 << 24) | (OODLE2_VERSION_MAJOR << 16) | (OODLE2_VERSION_MINOR << 8) | sizeof(OodleLZ_SeekTable)
        // The version string is:
        //     2 . OODLE2_VERSION_MAJOR . OODLE2_VERSION_MINOR
        return String.format("2.%d.%d", version >>> 16 & 0xff, version >>> 8 & 0xff);
    }

    public static int getCompressedSize(int size) {
        return size + 274 * getBlocksCount(size);
    }

    public static int getBlocksCount(long size) {
        return (int) ((size + BLOCK_SIZE_BYTES - 1) / BLOCK_SIZE_BYTES);
    }

    @Override
    public void close() {
        synchronized (library) {
            if (useCount <= 0) {
                throw new IllegalStateException("Compressor is disposed");
            }

            useCount -= 1;

            if (useCount == 0) {
                library.dispose();
                compressors.remove(path);
            }
        }
    }

    @Override
    public String toString() {
        return "Compressor{path=" + path + ", version=" + getVersionString() + '}';
    }

    public enum Level {
        // @formatter:off
        /** Don't compress, just copy raw bytes */
        NONE(0),
        /** Super fast mode, lower compression ratio */
        SUPER_FAST(1),
        /** Fastest LZ mode with still decent compression ratio */
        VERY_FAST(2),
        /** Fast - good for daily use */
        FAST(3),
        /** Standard medium speed LZ mode */
        NORMAL(4),
        /** Optimal parse level 1 (faster optimal encoder) */
        OPTIMAL_1(5),
        /** Optimal parse level 2 (recommended baseline optimal encoder) */
        OPTIMAL_2(6),
        /** Optimal parse level 3 (slower optimal encoder) */
        OPTIMAL_3(7),
        /** Optimal parse level 4 (very slow optimal encoder) */
        OPTIMAL_4(8),
        /** Optimal parse level 5 (don't care about encode speed, maximum compression) */
        OPTIMAL_5(9),
        /** Faster than {@link Level#SUPER_FAST}, less compression */
        HYPER_FAST_1(-1),
        /** Faster than {@link Level#HYPER_FAST_1}, less compression */
        HYPER_FAST_2(-2),
        /** Faster than {@link Level#HYPER_FAST_2}, less compression */
        HYPER_FAST_3(-3),
        /** Fastest, less compression */
        HYPER_FAST_4(-4);
        // @formatter:on

        private final int value;

        Level(int value) {
            this.value = value;
        }
    }

    private interface OodleLibrary extends Library {
        int OodleLZ_Compress(int compressor, byte[] rawBuf, long rawLen, byte[] compBuf, int level, long pOptions, long dictionaryBase, long lrm, long scratchMem, long scratchSize);

        int OodleLZ_Decompress(byte[] compBuf, long compBufSize, byte[] rawBuf, long rawLen, int fuzzSafe, int checkCRC, int verbosity, long decBufBase, long decBufSize, long fpCallback, long callbackUserData, long decoderMemory, long decoderMemorySize, int threadPhase);

        void Oodle_GetConfigValues(int[] buffer);

        void dispose();
    }
}
