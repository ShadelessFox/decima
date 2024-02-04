package com.shade.decima.model.packfile.prefetch;

import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.model.util.FilePath;
import com.shade.util.NotNull;

public record PrefetchChangeInfo(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {}
