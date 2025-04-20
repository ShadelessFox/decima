package com.shade.decima.ui.navigator;

import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(id = "navigator", name = "Navigator")
public class NavigatorSettingsPage implements SettingsPage {
    private JComboBox<NavigatorSettings.ArchiveView> archiveViewCombo;
    private JComboBox<NavigatorSettings.DirectoryView> directoryViewCombo;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Structure"));
        panel.setLayout(new MigLayout("ins panel,wrap", "[fill][]"));

        panel.add(new JLabel("Archive view mode:"));
        panel.add(archiveViewCombo = new JComboBox<>(NavigatorSettings.ArchiveView.values()), "split 2");
        panel.add(UIUtils.createHelpToolTip("""
            <html>
            Depending on the choice, archives will be shown differently:<br>
            <b>Default</b> - Individual archives will be shown separately<br>
            <b>Grouped</b> - Archives will be grouped by their name<br>
            <b>Merged</b> - Files from all archives will be shown together
            """));

        panel.add(new JLabel("Directory view mode:"));
        panel.add(directoryViewCombo = new JComboBox<>(NavigatorSettings.DirectoryView.values()), "split 2");
        panel.add(UIUtils.createHelpToolTip("""
            <html>
            Depending on the choice, directories will be shown differently:<br>
            <b>Default</b> - Files and directories will be shown separately<br>
            <b>Flatten</b> - All directories will be flattened into a single list<br>
            <b>Compact</b> - Empty directories will be flattened into a single list
            """));

        panel.add(UIUtils.createInfoLabel("Changes to opened projects will take effect upon reopening"), "span");

        // FIXME Not fancy
        final ItemListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        archiveViewCombo.addItemListener(adapter);
        directoryViewCombo.addItemListener(adapter);

        return panel;
    }

    @Override
    public void apply() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        settings.archiveView = archiveViewCombo.getItemAt(archiveViewCombo.getSelectedIndex());
        settings.directoryView = directoryViewCombo.getItemAt(directoryViewCombo.getSelectedIndex());
    }

    @Override
    public void reset() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        archiveViewCombo.setSelectedItem(settings.archiveView);
        directoryViewCombo.setSelectedItem(settings.directoryView);
    }

    @Override
    public boolean isModified() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        return settings.archiveView != archiveViewCombo.getItemAt(archiveViewCombo.getSelectedIndex())
            || settings.directoryView != directoryViewCombo.getItemAt(directoryViewCombo.getSelectedIndex());
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
