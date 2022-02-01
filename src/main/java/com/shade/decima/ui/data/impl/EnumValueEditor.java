package com.shade.decima.ui.data.impl;

import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;

public class EnumValueEditor implements ValueEditor {
    public static final EnumValueEditor INSTANCE = new EnumValueEditor();

    private EnumValueEditor() {
    }

    @NotNull
    @Override
    public JComponent createComponent(@NotNull RTTIType<?> type) {
        final JComboBox<Object> combo = new JComboBox<>();

        for (RTTITypeEnum.Constant constant : ((RTTITypeEnum) type).getConstants()) {
            combo.addItem(constant);
        }

        return combo;
    }

    @Override
    public void setEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type, @NotNull Object value) {
        ((JComboBox<?>) component).setSelectedItem(value);
    }

    @Nullable
    @Override
    public Object getEditorValue(@NotNull JComponent component, @NotNull RTTIType<?> type) {
        return ((JComboBox<?>) component).getSelectedItem();
    }

    @Override
    public void addActionListener(@NotNull JComponent component, @NotNull ActionListener listener) {
        ((JComboBox<?>) component).addActionListener(listener);
    }

    @Override
    public void removeActionListener(@NotNull JComponent component, @NotNull ActionListener listener) {
        ((JComboBox<?>) component).removeActionListener(listener);
    }
}
