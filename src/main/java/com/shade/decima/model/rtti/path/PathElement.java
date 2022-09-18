package com.shade.decima.model.rtti.path;

import com.shade.util.NotNull;

public sealed interface PathElement permits PathElementIndex, PathElementName, PathElementUUID {
    @NotNull
    Object get(@NotNull Object object);

    void set(@NotNull Object object, @NotNull Object value);
}
