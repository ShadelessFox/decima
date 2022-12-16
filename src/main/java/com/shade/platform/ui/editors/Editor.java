package com.shade.platform.ui.editors;

import com.shade.util.NotNull;

import javax.swing.*;

public interface Editor {
    @NotNull
    JComponent createComponent();

    @NotNull
    EditorInput getInput();

    void setFocus();

    boolean isFocused();
}
