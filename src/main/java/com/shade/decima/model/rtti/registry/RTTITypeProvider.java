package com.shade.decima.model.rtti.registry;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import java.nio.file.Path;

public interface RTTITypeProvider {
    void initialize(@NotNull RTTITypeRegistry registry, @NotNull Path externalTypeInfo, @NotNull GameType gameType);

    @Nullable
    RTTIType<?> lookup(@NotNull RTTITypeRegistry registry, @NotNull String name);

    void resolve(@NotNull RTTITypeRegistry registry, @NotNull RTTIType<?> type);
}
