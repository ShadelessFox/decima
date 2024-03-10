package com.shade.decima.model.archive;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.Closeable;
import java.util.Collection;

public interface ArchiveManager extends Closeable {
    @Nullable
    ArchiveFile findFile(@NotNull String identifier);

    @NotNull
    ArchiveFile getFile(@NotNull String identifier);

    @NotNull
    Collection<? extends Archive> getArchives();
}
