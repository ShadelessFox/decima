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

    void addActionListener(@NotNull ActionListener listener);

    void removeActionListener(@NotNull ActionListener listener);
}
