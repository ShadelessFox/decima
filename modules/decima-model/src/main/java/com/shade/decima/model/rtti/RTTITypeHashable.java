package com.shade.decima.model.rtti;

import com.shade.util.NotNull;

public interface RTTITypeHashable<T_INSTANCE> {
    int getHash(@NotNull T_INSTANCE instance);
}
