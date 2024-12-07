package com.shade.decima.game;

import com.shade.util.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

public interface Archive extends Closeable {
    @NotNull
    Asset get(@NotNull AssetId id) throws IOException;

    @NotNull
    ByteBuffer load(@NotNull AssetId id) throws IOException;

    boolean contains(@NotNull AssetId id);

    @NotNull
    List<? extends Asset> assets();

    @NotNull
    String name();

    @NotNull
    Path path();
}
