package com.shade.decima.model.app;

import com.shade.util.NotNull;

public interface ProjectChangeListener {
    default void projectAdded(@NotNull ProjectContainer container) {
        // do nothing by default
    }

    default void projectUpdated(@NotNull ProjectContainer container) {
        // do nothing by default
    }

    default void projectRemoved(@NotNull ProjectContainer container) {
        // do nothing by default
    }

    default void projectClosed(@NotNull ProjectContainer container) {
        // do nothing by default
    }
}
