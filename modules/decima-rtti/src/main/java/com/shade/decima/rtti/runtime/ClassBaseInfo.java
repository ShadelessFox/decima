package com.shade.decima.rtti.runtime;

import com.shade.util.NotNull;

public record ClassBaseInfo(
    @NotNull TypeInfoRef type,
    int offset
) {
}
