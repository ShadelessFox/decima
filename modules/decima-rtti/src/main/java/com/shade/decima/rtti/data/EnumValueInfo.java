package com.shade.decima.rtti.data;

import com.shade.util.NotNull;

public record EnumValueInfo(
    @NotNull String name,
    int value
) {
}
