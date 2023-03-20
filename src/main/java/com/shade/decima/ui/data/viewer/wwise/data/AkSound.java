package com.shade.decima.ui.data.viewer.wwise.data;

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
