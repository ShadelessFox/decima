package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public record ClassAttrInfo(
    @NotNull String name,
    @Nullable String category,
    @NotNull TypeInfoRef type,
    @Nullable String min,
    @Nullable String max,
    int position,
    int offset,
    int flags,
    boolean property
) {
}
