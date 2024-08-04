package com.shade.decima.ui.data.viewer.audio.data.wwise;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record AkSound(int id, @NotNull AkBankSourceData source) implements AkHircNode {
    @NotNull
    public static AkSound read(@NotNull ByteBuffer buffer) {
        return new AkSound(
            buffer.getInt(),
            AkBankSourceData.read(buffer)
        );
    }
}
