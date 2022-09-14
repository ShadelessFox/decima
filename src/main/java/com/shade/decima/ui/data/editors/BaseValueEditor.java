package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;

public abstract class BaseValueEditor<T, C extends JComponent> implements ValueEditor<T> {
    protected final ValueController<T> controller;
    protected C component;

    public BaseValueEditor(@NotNull ValueController<T> controller) {
        this.controller = controller;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return component = createComponentImpl();
    }

    @Override
    public void addActionListener(@NotNull ActionListener listener) {
        // do nothing by default
    }

    @Override
    public void removeActionListener(@NotNull ActionListener listener) {
        // do nothing by default
    }

    @NotNull
    protected abstract C createComponentImpl();
}
