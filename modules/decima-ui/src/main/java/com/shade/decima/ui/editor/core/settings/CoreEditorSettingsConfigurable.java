package com.shade.decima.ui.editor.core.settings;

import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(id = "coreEditor", name = "Core Editor")
public class CoreEditorSettingsConfigurable implements SettingsPage {
    private JCheckBox showBreadcrumbsCheckbox;
    private JCheckBox showValuePanelCheckbox;
    private JCheckBox selectFirstEntryCheckbox;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setBorder(new LabeledBorder("Appearance"));
        panel.setLayout(new MigLayout("ins panel", "[grow,fill,400lp]"));

        panel.add(showBreadcrumbsCheckbox = new JCheckBox("Show breadcrumbs"), "wrap");
        panel.add(showValuePanelCheckbox = new JCheckBox("Show value panel automatically"), "wrap");
        panel.add(selectFirstEntryCheckbox = new JCheckBox("Select first entry"), "wrap");

        // FIXME Not fancy
        final ItemListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        showBreadcrumbsCheckbox.addItemListener(adapter);
        showValuePanelCheckbox.addItemListener(adapter);
        selectFirstEntryCheckbox.addItemListener(adapter);

        return panel;
    }

    @Override
    public void apply() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        settings.showBreadcrumbs = showBreadcrumbsCheckbox.isSelected();
        settings.showValuePanel = showValuePanelCheckbox.isSelected();
        settings.selectFirstEntry = selectFirstEntryCheckbox.isSelected();

        MessageBus.getInstance().publisher(CoreEditorSettings.SETTINGS).settingsChanged();
    }

    @Override
    public void reset() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        showBreadcrumbsCheckbox.setSelected(settings.showBreadcrumbs);
        showValuePanelCheckbox.setSelected(settings.showValuePanel);
        selectFirstEntryCheckbox.setSelected(settings.selectFirstEntry);
    }

    @Override
    public boolean isModified() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        return settings.showBreadcrumbs != showBreadcrumbsCheckbox.isSelected()
            || settings.showValuePanel != showValuePanelCheckbox.isSelected()
            || settings.selectFirstEntry != selectFirstEntryCheckbox.isSelected();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
