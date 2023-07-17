package com.shade.decima.ui.navigator;

import com.shade.decima.model.util.FilePath;
import com.shade.util.NotNull;

public record NavigatorPath(@NotNull String projectId, @NotNull String packfileId, @NotNull FilePath filePath) {}
