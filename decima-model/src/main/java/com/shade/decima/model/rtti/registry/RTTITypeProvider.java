package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;

public interface RTTITypeProvider {
    void initialize(@NotNull RTTITypeRegistry registry, @NotNull ProjectContainer container) throws IOException;

    @Nullable
    default RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name) {
        return null;
    }

    @Nullable
    default RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull Class<?> cls) {
        return null;
    }

    default void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type) {
        // do nothing by default
    }
}
