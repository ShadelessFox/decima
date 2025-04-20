package com.shade.platform.ui.controls;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@FunctionalInterface
public interface DocumentAdapter extends DocumentListener {
    void documentUpdated(DocumentEvent e);

    @Override
    default void insertUpdate(DocumentEvent e) {
        documentUpdated(e);
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
        documentUpdated(e);
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
        documentUpdated(e);
    }
}
