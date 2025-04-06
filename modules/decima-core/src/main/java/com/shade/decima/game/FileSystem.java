package com.shade.decima.game;

import com.shade.util.NotNull;

import java.nio.file.Path;

public interface FileSystem {
    @NotNull
    Path resolve(@NotNull String path);
}
