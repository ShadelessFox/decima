package com.shade.decima.ui.dialogs;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.validation.InputValidator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

public abstract class BaseEditDialog extends JDialog implements PropertyChangeListener {
    public static final int OK_ID = 1;
    public static final int CANCEL_ID = 2;

    private final JButton okButton;
    private final JButton cancelButton;
    private int result;

    public BaseEditDialog(@Nullable JFrame owner, @NotNull String title) {
        super(owner);

        this.okButton = new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed(e);
            }
        });

        this.cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelPressed(e);
            }
        });

        final JComponent pane = (JComponent) getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(createContentsPane(), BorderLayout.CENTER);
        pane.add(createButtonsPane(), BorderLayout.SOUTH);
        pane.registerKeyboardAction(this::cancelPressed, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();
        setModal(true);
        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(getOwner());

        updateCompletion();
    }

    @NotNull
    private Component createButtonsPane() {
        getRootPane().setDefaultButton(okButton);

        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets dialog,alignx right", ""));
        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(InputValidator.PROPERTY_VALIDATION)) {
            updateCompletion();
        }
    }

    public int open() {
        setVisible(true);
        return result;
    }

    protected void updateCompletion() {
        getOkButton().setEnabled(isComplete());
    }

    public abstract void load(@Nullable Preferences preferences);

    public abstract void save(@Nullable Preferences preferences);

    public abstract boolean isComplete();

    @NotNull
    public JButton getOkButton() {
        return okButton;
    }

    @NotNull
    public JButton getCancelButton() {
        return cancelButton;
    }

    @NotNull
    protected abstract JComponent createContentsPane();

    protected void okPressed(@NotNull ActionEvent event) {
        result = OK_ID;
        dispose();
    }

    protected void cancelPressed(@NotNull ActionEvent event) {
        result = CANCEL_ID;
        dispose();
    }
}
