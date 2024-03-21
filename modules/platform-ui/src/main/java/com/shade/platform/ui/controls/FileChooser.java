package com.shade.platform.ui.controls;

import com.shade.util.Nullable;

import javax.swing.*;
import java.io.File;

public class FileChooser extends JFileChooser {
    public static final String OPTIONS_CHANGED_PROPERTY = "optionsChanged";

    private JComponent options;

    @Override
    public void approveSelection() {
        final File file = getSelectedFile();

        if (file != null && file.exists() && getDialogType() == SAVE_DIALOG) {
            final int result = JOptionPane.showConfirmDialog(
                this,
                "File '%s' already exists. Do you want to overwrite it?".formatted(file.getName()),
                "Confirm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }
        }

        super.approveSelection();
    }

    @Nullable
    public JComponent getOptions() {
        return options;
    }

    public void setOptions(@Nullable JComponent options) {
        final JComponent oldValue = this.options;
        this.options = options;
        firePropertyChange(OPTIONS_CHANGED_PROPERTY, oldValue, options);
    }
}
