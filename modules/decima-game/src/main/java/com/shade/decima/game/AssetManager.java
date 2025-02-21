package com.shade.decima.game;

import com.shade.util.NotNull;

import java.io.IOException;

public interface AssetManager {
    @NotNull
    <T> T get(@NotNull AssetId id, @NotNull Class<T> type) throws IOException;
}
