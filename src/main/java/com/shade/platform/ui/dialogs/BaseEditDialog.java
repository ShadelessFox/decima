package com.shade.platform.ui.dialogs;

import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

public abstract class BaseEditDialog extends BaseDialog implements PropertyChangeListener {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public BaseEditDialog(@NotNull String title, @NotNull List<ButtonDescriptor> buttons) {
        super(title, buttons);
    }

    public BaseEditDialog(@NotNull String title) {
        super(title);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        pcs.firePropertyChange(event);
    }

    @NotNull
    @Override
    protected JDialog createDialog(@Nullable JFrame owner) {
        final JDialog dialog = super.createDialog(owner);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // Trigger button update if initial configuration is invalid
                pcs.firePropertyChange(InputValidator.PROPERTY_VALIDATION, null, null);
            }
        });

        return dialog;
    }

    @Override
    protected void configureButton(@NotNull JButton button, @NotNull ButtonDescriptor descriptor) {
        pcs.addPropertyChangeListener(InputValidator.PROPERTY_VALIDATION, event -> button.setEnabled(isButtonEnabled(descriptor)));

        super.configureButton(button, descriptor);
    }

    protected boolean isButtonEnabled(@NotNull ButtonDescriptor descriptor) {
        return descriptor == BUTTON_CANCEL || isComplete();
    }

    protected abstract boolean isComplete();
}