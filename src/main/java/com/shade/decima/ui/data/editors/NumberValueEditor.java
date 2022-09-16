package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueController.EditType;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionListener;

public class NumberValueEditor extends BaseValueEditor<Number, JTextComponent> {
    private final long min;
    private final long max;

    public NumberValueEditor(@NotNull ValueController<Number> controller, long min, long max) {
        super(controller);
        this.min = min;
        this.max = max;
    }

    @NotNull
    @Override
    protected JTextComponent createComponentImpl() {
        if (controller.getEditType() == EditType.INLINE) {
            return new JTextField();
        } else {
            return new JTextArea();
        }
    }

    @Override
    public void setEditorValue(@NotNull Number value) {
        component.setText(String.valueOf(value));
    }

    @NotNull
    @Override
    public Number getEditorValue() {
        final long value = Long.parseLong(component.getText());

        if (value < min || value > max) {
            throw new NumberFormatException("Value out of range");
        }

        if (Byte.MIN_VALUE <= min && Byte.MAX_VALUE >= max) {
            return (byte) value;
        } else if (Short.MIN_VALUE <= min && Short.MAX_VALUE >= max) {
            return (short) value;
        } else if (Integer.MIN_VALUE <= min && Integer.MAX_VALUE >= max) {
            return (int) value;
        } else {
            return value;
        }
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
