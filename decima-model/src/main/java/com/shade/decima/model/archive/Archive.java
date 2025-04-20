package com.shade.decima.model.archive;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.Closeable;
import java.nio.file.Path;

public interface Archive extends Closeable {
    @NotNull
    ArchiveManager getManager();

    @NotNull
    String getId();

    @NotNull
    String getName();

    @NotNull
    Path getPath();

    @Nullable
    ArchiveFile findFile(@NotNull String identifier);

    @Nullable
    ArchiveFile findFile(long identifier);

    @NotNull
    default ArchiveFile getFile(@NotNull String identifier) {
        final ArchiveFile file = findFile(identifier);
        if (file == null) {
            throw new IllegalArgumentException("Can't find file '%s' in archive %s".formatted(identifier, getName()));
        }
        return file;
    }

    @NotNull
    default ArchiveFile getFile(long identifier) {
        final ArchiveFile file = findFile(identifier);
        if (file == null) {
            throw new IllegalArgumentException("Can't find file '%#018x' in archive %s".formatted(identifier, getName()));
        }
        return file;
    }
}
