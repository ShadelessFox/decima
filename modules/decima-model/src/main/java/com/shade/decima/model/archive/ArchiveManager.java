package com.shade.decima.model.archive;

import com.shade.util.NotNull;

import java.io.Closeable;
import java.util.Collection;

public interface ArchiveManager extends Closeable {
    @NotNull
    ArchiveFile getFile(@NotNull String identifier);

    @NotNull
    Collection<? extends Archive> getArchives();
}
