package com.shade.decima.ui.controls.validation;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.beans.PropertyChangeSupport;

public abstract class InputValidator extends InputVerifier {
    public static final String PROPERTY_VALIDATION = "validation";

    private final PropertyChangeSupport propertyChangeSupport;
    private final JComponent component;
    private Validation validation;

    public InputValidator(@NotNull JComponent component) {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.component = component;

        if (component instanceof JTextComponent text) {
            addChangeListener(text, e -> verify(this.component));
        }

        verify(this.component);
    }

    @Override
    public final boolean verify(JComponent input) {
        final Validation oldValidation = validation;

        validation = validate(input);

        if (!validation.equals(oldValidation)) {
            input.putClientProperty(FlatClientProperties.OUTLINE, validation.type().getOutline());
            input.setToolTipText(validation.message());
            propertyChangeSupport.firePropertyChange(InputValidator.PROPERTY_VALIDATION, oldValidation, validation);
        }

        return validation.isOK();
    }

    @Override
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        verify(source);
        return true;
    }

    @NotNull
    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @Nullable
    public Validation getLastValidation() {
        return validation;
    }

    @NotNull
    protected abstract Validation validate(@NotNull JComponent input);

    private void addChangeListener(@NotNull JTextComponent component, @NotNull ChangeListener changeListener) {
        final DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changeListener.stateChanged(new ChangeEvent(component));
            }
        };

        component.addPropertyChangeListener("document", event -> {
            final Document oldDocument = (Document) event.getOldValue();
            final Document newDocument = (Document) event.getNewValue();

            if (oldDocument != null) {
                oldDocument.removeDocumentListener(listener);
            }

            if (newDocument != null) {
                newDocument.addDocumentListener(listener);
            }

            listener.changedUpdate(null);
        });

        component.getDocument().addDocumentListener(listener);
    }
}
