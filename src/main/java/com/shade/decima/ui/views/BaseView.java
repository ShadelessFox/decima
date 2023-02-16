package com.shade.decima.ui.views;

import com.shade.platform.ui.views.View;
import com.shade.util.NotNull;

import javax.swing.*;

public abstract class BaseView<C extends JComponent> implements View {
    protected C component;

    @NotNull
    @Override
    public JComponent createComponent() {
        return component = createComponentImpl();
    }

    @Override
    public void setFocus() {
        component.requestFocusInWindow();
    }

    @NotNull
    protected abstract C createComponentImpl();
}
