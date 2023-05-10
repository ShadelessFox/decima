package com.shade.decima.ui.data.editors;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueEditor;
import com.shade.util.NotNull;

import javax.swing.*;

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

    @NotNull
    protected abstract C createComponentImpl();
}
