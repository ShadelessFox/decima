package com.shade.decima.ui.data;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;

public interface ValueEditor<T> {
    @NotNull
    JComponent createComponent();

    void setEditorValue(@NotNull T value);

    @NotNull
    T getEditorValue();

    default void addActionListener(@NotNull ActionListener listener) {
        // do nothing by default
    }

    default void removeActionListener(@NotNull ActionListener listener) {
        // do nothing by default
    }
}
