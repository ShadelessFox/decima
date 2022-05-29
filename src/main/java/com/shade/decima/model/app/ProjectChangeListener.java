package com.shade.decima.model.app;

import com.shade.decima.model.util.NotNull;

public interface ProjectChangeListener {
    default void projectAdded(@NotNull Project project) {
        // do nothing by default
    }

    default void projectRemoved(@NotNull Project project) {
        // do nothing by default
    }
}
