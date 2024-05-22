package com.shade.decima.model.viewer.settings;

import com.shade.platform.ui.controls.validation.InputValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(parent = "coreEditor", id = "modelViewer", name = "Model Viewer")
public class ModelViewerSettingsPage implements SettingsPage {
    private JSpinner fieldOfViewSpinner;
    private JSpinner sensitivitySpinner;
    private JSpinner nearClipSpinner;
    private JSpinner farClipSpinner;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        final JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins panel,wrap", "[fill][fill]", ""));

        panel.add(new JLabel("Sensitivity:"));
        panel.add(sensitivitySpinner = new JSpinner(new SpinnerNumberModel((Number) 1.0f, 0.01f, 10.0f, 0.1f)));
        panel.add(new JLabel("Field of view:"));
        panel.add(fieldOfViewSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 150, 1)));
        panel.add(new JLabel("Near clip plane:"));
        panel.add(nearClipSpinner = new JSpinner(new SpinnerNumberModel((Number) 0.1f, 0.01f, 10000.0f, 1.0f)));
        panel.add(new JLabel("Far clip plane:"));
        panel.add(farClipSpinner = new JSpinner(new SpinnerNumberModel(10000.0f, 100.0f, null, 1.0f)));

        // FIXME Not fancy
        final ChangeListener adapter = e -> listener.propertyChange(new PropertyChangeEvent(this, InputValidator.PROPERTY_VALIDATION, null, null));
        fieldOfViewSpinner.addChangeListener(adapter);
        sensitivitySpinner.addChangeListener(adapter);
        nearClipSpinner.addChangeListener(adapter);
        farClipSpinner.addChangeListener(adapter);

        return panel;
    }

    @Override
    public void apply() {
        final ModelViewerSettings settings = ModelViewerSettings.getInstance();
        settings.fieldOfView = (int) fieldOfViewSpinner.getValue();
        settings.sensitivity = (float)sensitivitySpinner.getValue();
        settings.nearClip = (float)nearClipSpinner.getValue();
        settings.farClip = (float)farClipSpinner.getValue();
    }

    @Override
    public void reset() {
        final ModelViewerSettings settings = ModelViewerSettings.getInstance();
        fieldOfViewSpinner.setValue(settings.fieldOfView);
        sensitivitySpinner.setValue(settings.sensitivity);
        nearClipSpinner.setValue(settings.nearClip);
        farClipSpinner.setValue(settings.farClip);
    }

    @Override
    public boolean isModified() {
        final ModelViewerSettings settings = ModelViewerSettings.getInstance();
        return settings.fieldOfView != (int) fieldOfViewSpinner.getValue()
            || settings.sensitivity != (float) sensitivitySpinner.getValue()
            || settings.nearClip != (float) nearClipSpinner.getValue()
            || settings.farClip != (float) farClipSpinner.getValue();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
