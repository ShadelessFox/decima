package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

public interface RTTITypeProvider {
    void initialize(@NotNull RTTITypeRegistry registry, @NotNull ProjectContainer container);

    @Nullable
    RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name);

    void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type);
}
