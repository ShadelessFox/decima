package com.shade.decima.rtti.generator.data;

import com.shade.util.NotNull;

public record ClassBaseInfo(
    @NotNull ClassTypeInfo type,
    int offset
) {
}
