package com.shade.decima.ui.data.viewer.audio.data.wwise;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record AkMediaInformation(int sourceId, int inMemoryMediaSize, byte sourceBits) {
    @NotNull
    public static AkMediaInformation read(@NotNull ByteBuffer buffer) {
        return new AkMediaInformation(
            buffer.getInt(),
            buffer.getInt(),
            buffer.get()
        );
    }
}
