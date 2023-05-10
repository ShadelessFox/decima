package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueController.EditType;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;

public class StringValueEditor extends BaseValueEditor<String, JTextComponent> {
    public StringValueEditor(@NotNull ValueController<String> controller) {
        super(controller);
    }

    @NotNull
    @Override
    protected JTextComponent createComponentImpl() {
        if (controller.getEditType() == EditType.INLINE) {
            return new JTextField();
        } else {
            final JTextArea area = new JTextArea();
            area.setPreferredSize(new Dimension(500, 400));
            area.setLineWrap(true);
            return area;
        }
    }

    @Override
    public void setEditorValue(@NotNull String value) {
        component.setText(value);
    }

    @NotNull
    @Override
    public String getEditorValue() {
        return component.getText();
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        if (component instanceof JTextField field) {
            field.addActionListener(listener);
        }
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        if (component instanceof JTextField field) {
            field.removeActionListener(listener);
        }
    }
}
