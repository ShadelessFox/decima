package com.shade.decima.rtti.registry;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.nio.file.Path;

public interface RTTITypeProvider {
    void initialize(@NotNull RTTITypeRegistry registry, @NotNull Path externalTypeInfo);

    @Nullable
    RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name);

    void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type);
}
