package com.shade.decima.model.packfile.edit;

import com.shade.decima.model.packfile.resource.BufferResource;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.util.NotNull;

public record MemoryChange(@NotNull byte[] data, long hash) implements Change {
    @NotNull
    @Override
    public Change merge(@NotNull Change other) {
        if (other instanceof MemoryChange) {
            return other;
        } else {
            throw new IllegalArgumentException("Can't merge with " + other);
        }
    }

    @NotNull
    @Override
    public Resource toResource() {
        return new BufferResource(data, hash);
    }
}
