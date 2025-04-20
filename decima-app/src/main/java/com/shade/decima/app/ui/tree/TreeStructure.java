package com.shade.decima.app.ui.tree;

import com.shade.util.NotNull;

import java.util.List;

public interface TreeStructure<T> {
    @NotNull
    T getRoot();

    @NotNull
    List<? extends T> getChildren(@NotNull T parent);

    boolean hasChildren(@NotNull T parent);
}
