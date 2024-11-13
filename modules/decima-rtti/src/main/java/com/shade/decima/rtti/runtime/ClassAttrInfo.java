package com.shade.decima.rtti.runtime;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.invoke.VarHandle;

public record ClassAttrInfo(
    @NotNull String name,
    @Nullable String category,
    @NotNull TypeInfoRef type,
    @NotNull VarHandle handle,
    int offset,
    int flags
) {
    public Object get(@NotNull Object object) {
        return handle.get(object);
    }

    public void set(@NotNull Object object, Object value) {
        handle.set(object, value);
    }
}
