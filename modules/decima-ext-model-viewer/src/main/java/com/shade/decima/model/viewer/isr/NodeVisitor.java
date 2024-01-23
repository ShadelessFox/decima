package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface NodeVisitor<T> {
    @Nullable
    T visit(@NotNull Node node);
}
