package com.shade.decima.ui.data.viewer.audio.wwise;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record AkMusicTrack(int id, byte flags, @NotNull AkBankSourceData[] sources) implements AkHircNode {
    @NotNull
    public static AkMusicTrack read(@NotNull ByteBuffer buffer) {
        return new AkMusicTrack(
            buffer.getInt(),
            buffer.get(),
            IOUtils.getObjects(buffer, buffer.getInt(), AkBankSourceData[]::new, AkBankSourceData::read)
        );
    }
}
