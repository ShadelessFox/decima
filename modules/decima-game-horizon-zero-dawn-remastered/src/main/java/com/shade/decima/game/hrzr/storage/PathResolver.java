package com.shade.decima.game.hrzr.storage;

import com.shade.util.NotNull;

import java.nio.file.Path;

public interface PathResolver {
    @NotNull
    Path resolve(@NotNull String path);
}
