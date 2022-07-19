package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;

import javax.swing.*;

public interface Editor {
    @NotNull
    JComponent createComponent();

    @NotNull
    EditorInput getInput();

    @NotNull
    EditorController getController();
}
