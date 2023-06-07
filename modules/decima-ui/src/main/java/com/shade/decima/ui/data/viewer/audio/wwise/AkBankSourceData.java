package com.shade.decima.ui.data.viewer.audio.wwise;

import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record AkBankSourceData(int pluginId, @NotNull AkBankSourceType type, @NotNull AkMediaInformation info) {
    @NotNull
    public static AkBankSourceData read(@NotNull ByteBuffer buffer) {
        return new AkBankSourceData(
            buffer.getInt(),
            AkBankSourceType.read(buffer),
            AkMediaInformation.read(buffer)
        );
    }
}
