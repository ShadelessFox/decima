package com.shade.decima.model.util;

import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Compressor {
    enum Level {
        NONE,
        FAST,
        NORMAL,
        BEST
    }

    @NotNull
    ByteBuffer compress(@NotNull ByteBuffer src, @NotNull Level level) throws IOException;

    void decompress(@NotNull ByteBuffer src, @NotNull ByteBuffer dst) throws IOException;
}
