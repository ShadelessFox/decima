package com.shade.decima.model.packfile;

import com.shade.decima.model.util.CloseableLibrary;
import com.shade.decima.model.util.Compressor;
import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Oodle implements Compressor, Closeable {
    private static final Map<Path, Reference<Oodle>> compressors = new ConcurrentHashMap<>();

    private final OodleLibrary library;
    private final Path path;
    private volatile int useCount;

    private Oodle(@NotNull Path path) {
        this.library = CloseableLibrary.load(path.toString(), OodleLibrary.class);
        this.path = path;
        this.useCount = 1;
    }

    @NotNull
    public static Oodle acquire(@NotNull Path path) {
        final Reference<Oodle> ref = compressors.get(path);
        Oodle oodle = ref != null ? ref.get() : null;

        if (oodle == null) {
            oodle = new Oodle(path);
            compressors.put(path, new WeakReference<>(oodle));
        } else {
            synchronized (oodle.library) {
                oodle.useCount += 1;
            }
        }

        return oodle;
    }

    @NotNull
    @Override
    public ByteBuffer compress(@NotNull ByteBuffer src, @NotNull Level level) throws IOException {
        final var dst = ByteBuffer.allocate(getCompressedSize(src.remaining()));
        final var len = library.OodleLZ_Compress(8, src, src.remaining(), dst, getCompressionLevel(level), 0, 0, 0, 0, 0);
        if (len == 0) {
            throw new IOException("Error compressing data");
        }
        return dst.slice(0, len);
    }

    @Override
    public void decompress(@NotNull ByteBuffer src, @NotNull ByteBuffer dst) throws IOException {
        final int len = library.OodleLZ_Decompress(src, src.remaining(), dst, dst.remaining(), 1, 0, 0, 0, 0, 0, 0, 0, 0, 3);
        if (len != dst.remaining()) {
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

    private static int getCompressedSize(int size) {
        return size + 274 * Math.ceilDiv(size, 0x40000);
    }

    private static int getCompressionLevel(@NotNull Level level) {
        return switch (level) {
            case NONE -> /* NONE */ 0;
            case FAST -> /* SUPER_FAST */ 1;
            case NORMAL -> /* NORMAL */ 4;
            case BEST -> /* OPTIMAL_5 */ 9;
        };
    }

    @Override
    public void close() {
        synchronized (library) {
            if (useCount <= 0) {
                throw new IllegalStateException("Compressor is disposed");
            }

            useCount -= 1;

            if (useCount == 0) {
                library.close();
                compressors.remove(path);
            }
        }
    }

    @Override
    public String toString() {
        return "Compressor{path=" + path + ", version=" + getVersionString() + '}';
    }

    private interface OodleLibrary extends CloseableLibrary {
        int OodleLZ_Compress(int compressor, ByteBuffer rawBuf, long rawLen, ByteBuffer compBuf, int level, long pOptions, long dictionaryBase, long lrm, long scratchMem, long scratchSize);

        int OodleLZ_Decompress(ByteBuffer compBuf, long compBufSize, ByteBuffer rawBuf, long rawLen, int fuzzSafe, int checkCRC, int verbosity, long decBufBase, long decBufSize, long fpCallback, long callbackUserData, long decoderMemory, long decoderMemorySize, int threadPhase);

        void Oodle_GetConfigValues(int[] buffer);
    }
}
