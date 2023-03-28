package com.shade.decima.ui.editor.core.settings;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsKey;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

@SettingsPageRegistration(id = "coreEditor", name = "Core Editor")
public class CoreEditorSettings implements SettingsPage {
    public static final SettingsKey<Boolean> SHOW_BREADCRUMBS = SettingsKey.of("showBreadcrumbs", true);
    public static final SettingsKey<Boolean> SHOW_VALUE_PANEL = SettingsKey.of("showValuePanel", true);

    private JCheckBox showBreadcrumbsCheckbox;
    private JCheckBox showValuePanelCheckbox;

    @NotNull
    public static Preferences getPreferences() {
        return Application.getWorkspace().getPreferences().node("settings/coreEditor");
    }

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Appearance"));
        panel.setLayout(new MigLayout("ins panel", "[grow,fill,400lp]"));

        panel.add(showBreadcrumbsCheckbox = new JCheckBox("Show breadcrumbs"), "wrap");
        panel.add(showValuePanelCheckbox = new JCheckBox("Show value panel automatically"), "wrap");

        // FIXME Not fancy
        final ItemListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        showBreadcrumbsCheckbox.addItemListener(adapter);
        showValuePanelCheckbox.addItemListener(adapter);

        return panel;
    }

    @Override
    public void apply() {
        final Preferences pref = getPreferences();
        SHOW_BREADCRUMBS.set(pref, showBreadcrumbsCheckbox.isSelected());
        SHOW_VALUE_PANEL.set(pref, showValuePanelCheckbox.isSelected());
    }

    @Override
    public void reset() {
        final Preferences pref = getPreferences();
        showBreadcrumbsCheckbox.setSelected(SHOW_BREADCRUMBS.get(pref));
        showValuePanelCheckbox.setSelected(SHOW_VALUE_PANEL.get(pref));
    }

    @Override
    public boolean isModified() {
        final Preferences pref = getPreferences();
        return SHOW_BREADCRUMBS.get(pref) != showBreadcrumbsCheckbox.isSelected()
            || SHOW_VALUE_PANEL.get(pref) != showValuePanelCheckbox.isSelected();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
