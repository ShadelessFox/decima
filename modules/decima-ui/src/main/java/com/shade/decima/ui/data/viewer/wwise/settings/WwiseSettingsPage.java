package com.shade.decima.ui.data.viewer.wwise.settings;

import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.beans.PropertyChangeListener;

@SettingsPageRegistration(parent = "coreEditor", id = "wwise", name = "Wwise Audio")
public class WwiseSettingsPage implements SettingsPage {
    private JTextField ww2oggPath;
    private JTextField ww2oggCodebooksPath;
    private JTextField revorbPath;
    private JTextField ffmpegPath;

    @NotNull
    @Override
    public JComponent createComponent(@NotNull PropertyChangeListener listener) {
        {
            final FileExtensionFilter filter = new FileExtensionFilter("ww2ogg executable", "exe");

            ww2oggPath = new JTextField();
            UIUtils.addOpenFileAction(ww2oggPath, "Select ww2ogg executable", filter);
            UIUtils.installInputValidator(ww2oggPath, new ExistingFileValidator(ww2oggPath, filter, false), listener);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("ww2ogg codebooks", "bin");

            ww2oggCodebooksPath = new JTextField();
            UIUtils.addOpenFileAction(ww2oggCodebooksPath, "Select codebooks for ww2ogg", filter);
            UIUtils.installInputValidator(ww2oggCodebooksPath, new ExistingFileValidator(ww2oggCodebooksPath, filter, false), listener);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("revorb executable", "exe");

            revorbPath = new JTextField();
            UIUtils.addOpenFileAction(revorbPath, "Select revorb executable", filter);
            UIUtils.installInputValidator(revorbPath, new ExistingFileValidator(revorbPath, filter, false), listener);
        }

        {
            final FileExtensionFilter filter = new FileExtensionFilter("ffmpeg executable", "exe");

            ffmpegPath = new JTextField();
            UIUtils.addOpenFileAction(ffmpegPath, "Select ffmpeg executable", filter);
            UIUtils.installInputValidator(ffmpegPath, new ExistingFileValidator(ffmpegPath, filter, false), listener);
        }

        final JPanel tools = new JPanel();
        tools.setBorder(new LabeledBorder("Playback"));
        tools.setLayout(new MigLayout("ins panel", "[fill][grow,fill,400lp]", ""));

        tools.add(new JLabel("ww2ogg executable:"));
        tools.add(ww2oggPath, "wrap");

        tools.add(new JLabel("ww2ogg codebooks:"));
        tools.add(ww2oggCodebooksPath, "wrap");

        tools.add(new JLabel("revorb executable:"));
        tools.add(revorbPath, "wrap");

        tools.add(new JLabel("ffmpeg executable:"));
        tools.add(ffmpegPath);

        return tools;
    }

    @Override
    public void apply() {
        final WwiseSettings settings = WwiseSettings.getInstance();
        settings.ww2oggPath = ww2oggPath.getText().isEmpty() ? null : ww2oggPath.getText();
        settings.ww2oggCodebooksPath = ww2oggCodebooksPath.getText().isEmpty() ? null : ww2oggCodebooksPath.getText();
        settings.revorbPath = revorbPath.getText().isEmpty() ? null : revorbPath.getText();
        settings.ffmpegPath = ffmpegPath.getText().isEmpty() ? null : ffmpegPath.getText();
    }

    @Override
    public void reset() {
        final WwiseSettings settings = WwiseSettings.getInstance();
        ww2oggPath.setText(settings.ww2oggPath);
        ww2oggCodebooksPath.setText(settings.ww2oggCodebooksPath);
        revorbPath.setText(settings.revorbPath);
        ffmpegPath.setText(settings.ffmpegPath);
    }

    @Override
    public boolean isModified() {
        final WwiseSettings settings = WwiseSettings.getInstance();
        return !ww2oggPath.getText().equals(settings.ww2oggPath)
            || !ww2oggCodebooksPath.getText().equals(settings.ww2oggCodebooksPath)
            || !revorbPath.getText().equals(settings.revorbPath)
            || !ffmpegPath.getText().equals(settings.ffmpegPath);
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(ww2oggPath)
            && UIUtils.isValid(ww2oggCodebooksPath)
            && UIUtils.isValid(revorbPath)
            && UIUtils.isValid(ffmpegPath);
    }
}
