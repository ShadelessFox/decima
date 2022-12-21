package com.shade.decima.model.packfile.edit;

import com.shade.decima.model.packfile.resource.FileResource;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public record FileChange(@NotNull Path path, long hash) implements Change {
    @NotNull
    @Override
    public Change merge(@NotNull Change other) {
        if (other instanceof FileChange) {
            return other;
        } else {
            throw new IllegalArgumentException("Can't merge with " + other);
        }
    }

    @NotNull
    @Override
    public Resource toResource() throws IOException {
        return new FileResource(path, hash);
    }
}
