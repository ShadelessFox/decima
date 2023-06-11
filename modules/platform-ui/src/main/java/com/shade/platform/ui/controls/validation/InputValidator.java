package com.shade.platform.ui.controls.validation;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.platform.ui.controls.DocumentAdapter;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.beans.PropertyChangeSupport;

public abstract class InputValidator extends InputVerifier {
    public static final String PROPERTY_VALIDATION = "validation";

    private final PropertyChangeSupport propertyChangeSupport;
    private final JComponent component;
    private final JComponent overlay;
    private Validation validation;

    public InputValidator(@NotNull JComponent component, @NotNull JComponent overlay) {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.component = component;
        this.overlay = overlay;

        if (component instanceof JTextComponent text) {
            addChangeListener(text, e -> verify(this.component));
        } else if (component instanceof JTree tree) {
            tree.addTreeSelectionListener(e -> verify(this.component));
        } else if (component instanceof JList<?> list) {
            list.addListSelectionListener(e -> verify(this.component));
        } else {
            throw new IllegalArgumentException("Unsupported component: " + component);
        }

        verify(this.component);
    }

    public InputValidator(@NotNull JComponent component) {
        this(component, component);
    }

    @Override
    public final boolean verify(JComponent input) {
        final Validation oldValidation = validation;

        validation = validate(input);

        if (!validation.equals(oldValidation)) {
            overlay.putClientProperty(FlatClientProperties.OUTLINE, validation.type().getOutline());
            overlay.setToolTipText(validation.message());
            propertyChangeSupport.firePropertyChange(InputValidator.PROPERTY_VALIDATION, oldValidation, validation);
        } else {
            propertyChangeSupport.firePropertyChange(InputValidator.PROPERTY_VALIDATION, null, null);
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
        final DocumentListener listener = (DocumentAdapter) e -> changeListener.stateChanged(new ChangeEvent(component));

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
