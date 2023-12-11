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

@SettingsPageRegistration(id = "coreEditor", name = "Core Editor", order = 100)
public class CoreEditorSettingsPage implements SettingsPage {
    private JCheckBox showBreadcrumbsCheckbox;
    private JCheckBox showValuePanelCheckbox;
    private JCheckBox selectFirstEntryCheckbox;
    private JCheckBox groupEntriesCheckbox;
    private JCheckBox sortEntriesCheckbox;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel root = new JPanel();
        root.setLayout(new MigLayout("ins 0,wrap", "[grow,fill,400lp]"));

        {
            final JPanel panel = new JPanel();
            panel.setBorder(new LabeledBorder("Appearance"));
            panel.setLayout(new MigLayout("ins panel,wrap"));

            panel.add(showBreadcrumbsCheckbox = new JCheckBox("Show breadcrumbs"));
            panel.add(showValuePanelCheckbox = new JCheckBox("Show value panel automatically"));

            root.add(panel);
        }

        {
            final JPanel panel = new JPanel();
            panel.setBorder(new LabeledBorder("Entries"));
            panel.setLayout(new MigLayout("ins panel,wrap"));

            panel.add(selectFirstEntryCheckbox = new JCheckBox("Select first entry"));
            panel.add(groupEntriesCheckbox = new JCheckBox("Group entries by default"));
            panel.add(sortEntriesCheckbox = new JCheckBox("Sort entries by default"));

            root.add(panel);
        }

        // FIXME Not fancy
        final ItemListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        showBreadcrumbsCheckbox.addItemListener(adapter);
        showValuePanelCheckbox.addItemListener(adapter);
        selectFirstEntryCheckbox.addItemListener(adapter);
        groupEntriesCheckbox.addItemListener(adapter);
        sortEntriesCheckbox.addItemListener(adapter);

        return root;
    }

    @Override
    public void apply() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        settings.showBreadcrumbs = showBreadcrumbsCheckbox.isSelected();
        settings.showValuePanel = showValuePanelCheckbox.isSelected();
        settings.selectFirstEntry = selectFirstEntryCheckbox.isSelected();
        settings.groupEntries = groupEntriesCheckbox.isSelected();
        settings.sortEntries = sortEntriesCheckbox.isSelected();

        MessageBus.getInstance().publisher(CoreEditorSettings.SETTINGS).settingsChanged();
    }

    @Override
    public void reset() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        showBreadcrumbsCheckbox.setSelected(settings.showBreadcrumbs);
        showValuePanelCheckbox.setSelected(settings.showValuePanel);
        selectFirstEntryCheckbox.setSelected(settings.selectFirstEntry);
        groupEntriesCheckbox.setSelected(settings.groupEntries);
        sortEntriesCheckbox.setSelected(settings.sortEntries);
    }

    @Override
    public boolean isModified() {
        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        return settings.showBreadcrumbs != showBreadcrumbsCheckbox.isSelected()
            || settings.showValuePanel != showValuePanelCheckbox.isSelected()
            || settings.selectFirstEntry != selectFirstEntryCheckbox.isSelected()
            || settings.groupEntries != groupEntriesCheckbox.isSelected()
            || settings.sortEntries != sortEntriesCheckbox.isSelected();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
