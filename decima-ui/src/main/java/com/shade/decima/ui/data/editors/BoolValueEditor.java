package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.MutableValueController;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;

public class BoolValueEditor extends BaseValueEditor<Boolean, JCheckBox> {
    public BoolValueEditor(@NotNull MutableValueController<Boolean> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JCheckBox createComponentImpl() {
        return new JCheckBox();
    }

    @Override
    public void setEditorValue(@NotNull Boolean value) {
        component.setSelected(value);
    }

    @NotNull
    @Override
    public Boolean getEditorValue() {
        return component.isSelected();
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        component.addActionListener(listener);
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        component.removeActionListener(listener);
    }
}
