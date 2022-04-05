package com.shade.decima.ui.data.editor;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.data.ValueEditor;

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
