package com.shade.decima.ui.navigator;

import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(id = "navigator", name = "Navigator")
public class NavigatorSettingsPage implements SettingsPage {
    private JComboBox<NavigatorSettings.PackfileView> packfileViewCombo;
    private JComboBox<NavigatorSettings.DirectoryView> directoryViewCombo;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Structure"));
        panel.setLayout(new MigLayout("ins panel,wrap", "[fill][]"));

        panel.add(new JLabel("Packfile view mode:"));
        panel.add(packfileViewCombo = new JComboBox<>(NavigatorSettings.PackfileView.values()));

        panel.add(new JLabel("Directory view mode:"));
        panel.add(directoryViewCombo = new JComboBox<>(NavigatorSettings.DirectoryView.values()));

        final ColoredComponent tip = new ColoredComponent();
        tip.append("Changes to opened projects will take effect upon reopening", TextAttributes.GRAYED_SMALL_ATTRIBUTES);
        tip.setPadding(new Insets(1, 0, 1, 0));
        panel.add(tip, "span");

        // FIXME Not fancy
        final ItemListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        packfileViewCombo.addItemListener(adapter);
        directoryViewCombo.addItemListener(adapter);

        return panel;
    }

    @Override
    public void apply() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        settings.packfileView = packfileViewCombo.getItemAt(packfileViewCombo.getSelectedIndex());
        settings.directoryView = directoryViewCombo.getItemAt(directoryViewCombo.getSelectedIndex());
    }

    @Override
    public void reset() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        packfileViewCombo.setSelectedItem(settings.packfileView);
        directoryViewCombo.setSelectedItem(settings.directoryView);
    }

    @Override
    public boolean isModified() {
        final NavigatorSettings settings = NavigatorSettings.getInstance();
        return settings.packfileView != packfileViewCombo.getItemAt(packfileViewCombo.getSelectedIndex())
            || settings.directoryView != directoryViewCombo.getItemAt(directoryViewCombo.getSelectedIndex());
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
