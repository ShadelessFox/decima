package com.shade.decima.hfw.ui.editor.tree;

import com.shade.util.NotNull;

public interface TreeStructure {
    @NotNull
    Object getRoot();

    @NotNull
    Object[] getChildren(@NotNull Object element);

    boolean hasChildren(@NotNull Object element);
}
