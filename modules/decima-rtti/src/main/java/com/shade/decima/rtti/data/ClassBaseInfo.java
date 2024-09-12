package com.shade.decima.rtti.data;

import com.shade.util.NotNull;

public record ClassBaseInfo(
    @NotNull ClassTypeInfo type,
    int offset
) {
}
