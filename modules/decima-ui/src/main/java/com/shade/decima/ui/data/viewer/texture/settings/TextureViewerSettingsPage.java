package com.shade.decima.ui.data.viewer.texture.settings;

import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(parent = "coreEditor", id = "textureViewer", name = "Texture Viewer")
public class TextureViewerSettingsPage implements SettingsPage {
    private JCheckBox showGridCheckbox;
    private JSpinner showGridWhenZoomEqualOrMoreThanSpinner;
    private JSpinner showGridEveryNthPixelSpinner;
    private JCheckBox showOutline;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0"));

        panel.add(showGridCheckbox = new JCheckBox("Show grid", true), "wrap");

        panel.add(new JLabel("Show grid if zoom level is greater than:"), "gap ind");
        panel.add(showGridWhenZoomEqualOrMoreThanSpinner = new JSpinner(new SpinnerNumberModel(3, 1, null, 1)), "wrap");

        panel.add(new JLabel("Show grid lines between every:"), "gap ind");
        panel.add(showGridEveryNthPixelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1)));
        panel.add(new JLabel("pixels"), "wrap");

        panel.add(showOutline = new JCheckBox("Show image outline", true), "wrap");

        showGridCheckbox.addItemListener(e -> {
            final boolean selected = showGridCheckbox.isSelected();
            showGridWhenZoomEqualOrMoreThanSpinner.setEnabled(selected);
            showGridEveryNthPixelSpinner.setEnabled(selected);
        });

        // FIXME Not fancy
        final Runnable adapter = () -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        showGridCheckbox.addItemListener(e -> adapter.run());
        showGridWhenZoomEqualOrMoreThanSpinner.addChangeListener(e -> adapter.run());
        showGridEveryNthPixelSpinner.addChangeListener(e -> adapter.run());
        showOutline.addChangeListener(e -> adapter.run());

        return panel;
    }

    @Override
    public void apply() {
        final TextureViewerSettings settings = TextureViewerSettings.getInstance();
        settings.showGrid = showGridCheckbox.isSelected();
        settings.showGridWhenZoomEqualOrMoreThan = (int) showGridWhenZoomEqualOrMoreThanSpinner.getValue();
        settings.showGridEveryNthPixel = (int) showGridEveryNthPixelSpinner.getValue();
        settings.showOutline = showOutline.isSelected();

        MessageBus.getInstance().publisher(TextureViewerSettings.SETTINGS).settingsChanged();
    }

    @Override
    public void reset() {
        final TextureViewerSettings settings = TextureViewerSettings.getInstance();
        showGridCheckbox.setSelected(settings.showGrid);
        showGridWhenZoomEqualOrMoreThanSpinner.setValue(settings.showGridWhenZoomEqualOrMoreThan);
        showGridEveryNthPixelSpinner.setValue(settings.showGridEveryNthPixel);
        showOutline.setSelected(settings.showOutline);
    }

    @Override
    public boolean isModified() {
        final TextureViewerSettings settings = TextureViewerSettings.getInstance();
        return settings.showGrid != showGridCheckbox.isSelected()
            || settings.showGridWhenZoomEqualOrMoreThan != (int) showGridWhenZoomEqualOrMoreThanSpinner.getValue()
            || settings.showGridEveryNthPixel != (int) showGridEveryNthPixelSpinner.getValue()
            || settings.showOutline != showOutline.isSelected();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
