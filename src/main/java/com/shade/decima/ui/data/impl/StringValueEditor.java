package com.shade.decima.ui.data.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;

public class StringValueEditor implements ValueEditor {
    public static final StringValueEditor INSTANCE = new StringValueEditor();

    private StringValueEditor() {
    }

    @NotNull
    @Override
    public JComponent createComponent(@NotNull RTTIType<?> type) {
        return new JTextField();
    }

    @Override
    public void setEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type, @NotNull Object value) {
        ((JTextField) component).setText((String) value);
    }

    @Nullable
    @Override
    public Object getEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type) {
        return ((JTextField) component).getText();
    }

    @Override
    public void addActionListener(@NotNull JComponent component, @NotNull ActionListener listener) {
        ((JTextField) component).addActionListener(listener);
    }

    @Override
    public void removeActionListener(@NotNull JComponent component, @NotNull ActionListener listener) {
        ((JTextField) component).removeActionListener(listener);
    }
}
