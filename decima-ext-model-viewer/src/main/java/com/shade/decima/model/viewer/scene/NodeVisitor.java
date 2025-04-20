package com.shade.decima.model.viewer.scene;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface NodeVisitor<T> {
    @Nullable
    T visit(@NotNull Node node);
}
