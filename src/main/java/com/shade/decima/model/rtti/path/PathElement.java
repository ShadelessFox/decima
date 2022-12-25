package com.shade.decima.model.rtti.path;

import com.shade.util.NotNull;

public sealed interface PathElement permits PathElementIndex, PathElementField, PathElementEntry {
    @NotNull
    Object get(@NotNull Object object);

    void set(@NotNull Object object, @NotNull Object value);
}
