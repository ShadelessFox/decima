package com.shade.decima.ui.dialogs;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.validation.InputValidator;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public abstract class BaseEditDialog extends JComponent implements PropertyChangeListener {
    public static final int OK_ID = 1;
    public static final int CANCEL_ID = 2;

    private final JButton okButton;
    private final JButton cancelButton;
    private JDialog dialog;
    private int result;

    public BaseEditDialog() {
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
    }

    public int showDialog(@Nullable JFrame owner, @NotNull String title) {
        if (dialog != null) {
            throw new IllegalStateException("Dialog is open");
        }

        dialog = createDialog(owner, title);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                final JComponent component = getDefaultComponent();
                if (component != null) {
                    component.requestFocusInWindow();
                }
            }
        });

        updateCompletion();

        dialog.setVisible(true);

        dialog.getContentPane().removeAll();
        dialog.dispose();
        dialog = null;

        return result;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(InputValidator.PROPERTY_VALIDATION)) {
            updateCompletion();
        }
    }

    protected void updateCompletion() {
        getOkButton().setEnabled(isComplete());
    }

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

    @Nullable
    protected JButton getDefaultButton() {
        return okButton;
    }

    @Nullable
    protected JComponent getDefaultComponent() {
        return null;
    }

    protected void okPressed(@NotNull ActionEvent event) {
        if (dialog != null) {
            dialog.setVisible(false);
        }
        result = OK_ID;
    }

    protected void cancelPressed(@NotNull ActionEvent event) {
        if (dialog != null) {
            dialog.setVisible(false);
        }
        result = CANCEL_ID;
    }

    @NotNull
    private JDialog createDialog(@Nullable JFrame owner, @NotNull String title) {
        final JDialog dialog = new JDialog(owner, title, true);

        final JRootPane rootPane = dialog.getRootPane();
        rootPane.setDefaultButton(getDefaultButton());

        final JComponent contentPane = (JComponent) dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(createContentsPane(), BorderLayout.CENTER);
        contentPane.add(createButtonsPane(), BorderLayout.SOUTH);
        contentPane.registerKeyboardAction(this::cancelPressed, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        dialog.pack();
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(owner);

        return dialog;
    }

    @NotNull
    private Component createButtonsPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("insets dialog,alignx right", ""));
        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }
}
