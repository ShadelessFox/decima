package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;

public interface EditorInput {
    @NotNull
    String getName();

    @Nullable
    String getDescription();

    @Nullable
    Icon getIcon();

    @NotNull
    NavigatorFileNode getNode();

    @NotNull
    Project getProject();
}
