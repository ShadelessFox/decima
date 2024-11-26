package com.shade.decima.game.hrzr.storage.api;

import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface Archive extends Closeable {
    @NotNull
    List<? extends Asset> assets();

    @NotNull
    ByteBuffer load(@NotNull AssetId id) throws IOException;

    boolean contains(@NotNull AssetId id);
}
