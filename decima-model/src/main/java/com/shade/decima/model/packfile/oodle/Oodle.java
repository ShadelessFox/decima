package com.shade.decima.model.packfile.oodle;

import com.shade.decima.model.util.Compressor;
import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class Oodle implements Compressor, Closeable {
    private static final Map<Path, Reference<Oodle>> compressors = new ConcurrentHashMap<>();

    private final Arena arena;
    private final OodleFFM library;
    private final Path path;

    private volatile int useCount;

    private Oodle(@NotNull Path path) {
        this.arena = Arena.ofShared();
        this.library = new OodleFFM(path, arena);
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
            synchronized (oodle.arena) {
                oodle.useCount += 1;
            }
        }

        return oodle;
    }

    @NotNull
    @Override
    public ByteBuffer compress(@NotNull ByteBuffer src, @NotNull Level level) throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            var srcSegment = arena.allocate(src.remaining()).copyFrom(MemorySegment.ofBuffer(src));
            var dstSegment = arena.allocate(library.OodleLZ_GetCompressedBufferSizeNeeded(8, src.remaining()));

            var result = library.OodleLZ_Compress(
                8,
                srcSegment, srcSegment.byteSize(),
                dstSegment,
                getCompressionLevel(level),
                MemorySegment.NULL,
                MemorySegment.NULL,
                MemorySegment.NULL,
                MemorySegment.NULL,
                0
            );

            if (result == 0) {
                throw new IOException("Error compressing data");
            }

            var dst = ByteBuffer.allocate(Math.toIntExact(result));
            MemorySegment.ofBuffer(dst).copyFrom(dstSegment.reinterpret(result));

            return dst;
        }
    }

    @Override
    public void decompress(@NotNull ByteBuffer src, @NotNull ByteBuffer dst) throws IOException {
        try (var arena = Arena.ofConfined()) {
            var srcSegment = arena.allocate(src.remaining()).copyFrom(MemorySegment.ofBuffer(src));
            var dstSegment = arena.allocate(dst.remaining());

            var result = library.OodleLZ_Decompress(
                srcSegment, srcSegment.byteSize(),
                dstSegment, dstSegment.byteSize(),
                1, 1, 0,
                MemorySegment.NULL, 0,
                MemorySegment.NULL, MemorySegment.NULL,
                MemorySegment.NULL, 0,
                3
            );

            if (result != dst.remaining()) {
                throw new IOException("Error decompressing data");
            }

            MemorySegment.ofBuffer(dst).copyFrom(dstSegment);
        }
    }

    public int getVersion() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(JAVA_INT, 7);
            library.Oodle_GetConfigValues(segment);
            return segment.get(JAVA_INT, 24);
        }
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
        synchronized (arena) {
            if (useCount <= 0) {
                throw new IllegalStateException("Compressor is disposed");
            }

            useCount -= 1;

            if (useCount == 0) {
                arena.close();
                compressors.remove(path);
            }
        }
    }

    @Override
    public String toString() {
        return "Compressor{path=" + path + ", version=" + getVersionString() + '}';
    }
}
