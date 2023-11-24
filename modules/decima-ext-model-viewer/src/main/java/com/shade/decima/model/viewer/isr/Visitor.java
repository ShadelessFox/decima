package com.shade.decima.model.viewer.isr;

import com.shade.util.NotNull;
import com.shade.util.Nullable;

public interface Visitor {
    boolean enterNode(@NotNull Node node);

    @Nullable
    Node leaveNode(@NotNull Node node);
}
