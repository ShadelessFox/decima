package com.shade.decima.model.packfile;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.file.Path;

public record PackfileInfo(@NotNull Path path, @NotNull String name, @Nullable String language) {}
