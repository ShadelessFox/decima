package com.shade.platform.ui.editors;

import com.shade.platform.ui.Disposable;
import com.shade.util.NotNull;

import javax.swing.*;

public interface Editor extends Disposable {
    @NotNull
    JComponent createComponent();

    @NotNull
    EditorInput getInput();

    void setFocus();

    boolean isFocused();

    @Override
    default void dispose() {
        // do nothing by default
    }
}
