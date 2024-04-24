package com.shade.decima.model.archive;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.Closeable;
import java.util.Collection;

public interface ArchiveManager extends Closeable {
    @Nullable
    ArchiveFile findFile(@NotNull String identifier);

    @Nullable
    ArchiveFile findFile(long identifier);

    @NotNull
    default ArchiveFile getFile(@NotNull String identifier) {
        final ArchiveFile file = findFile(identifier);
        if (file == null) {
            throw new IllegalArgumentException("Can't find file '%s'".formatted(identifier));
        }
        return file;
    }

    @NotNull
    default ArchiveFile getFile(long identifier) {
        final ArchiveFile file = findFile(identifier);
        if (file == null) {
            throw new IllegalArgumentException("Can't find file '%#018x'".formatted(identifier));
        }
        return file;
    }

    @NotNull
    Collection<? extends Archive> getArchives();
}
