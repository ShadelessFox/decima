package com.shade.platform.ui.controls.plaf;

import com.shade.platform.ui.controls.FileChooser;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class FlatFileChooserUI extends com.formdev.flatlaf.ui.FlatFileChooserUI {
    private JComponent optionsPanel;

    public FlatFileChooserUI(JFileChooser fc) {
        super(fc);
    }

    public static ComponentUI createUI(JComponent c) {
        return new FlatFileChooserUI((JFileChooser) c);
    }

    @Override
    public void installComponents(JFileChooser fc) {
        super.installComponents(fc);

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BorderLayout());

        // Gaps and insets are defined according to MetalFileChooserUI.ButtonAreaLayout
        final JPanel buttonPanel = getButtonPanel();
        buttonPanel.setLayout(new MigLayout("ins 0,gapx 5,ins 17 0 0 0", "push[][fill][fill]"));
        buttonPanel.add(optionsPanel, 0);

        if (getFileChooser() instanceof FileChooser fc1 && fc1.getOptions() != null) {
            optionsPanel.add(fc1.getOptions(), BorderLayout.CENTER);
        }
    }

    @Override
    public void uninstallComponents(JFileChooser fc) {
        super.uninstallComponents(fc);

        optionsPanel = null;
    }

    @Override
    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        final PropertyChangeListener parent = super.createPropertyChangeListener(fc);
        return e -> {
            if (e.getPropertyName().equals(FileChooser.OPTIONS_CHANGED_PROPERTY)) {
                doOptionsChanged(e);
            } else {
                parent.propertyChange(e);
            }
        };
    }

    private void doOptionsChanged(@NotNull PropertyChangeEvent e) {
        if (optionsPanel != null) {
            if (e.getOldValue() != null) {
                optionsPanel.remove((JComponent) e.getOldValue());
            }
            if (e.getNewValue() != null) {
                optionsPanel.add((JComponent) e.getNewValue(), BorderLayout.CENTER);
            }
        }
    }
}
