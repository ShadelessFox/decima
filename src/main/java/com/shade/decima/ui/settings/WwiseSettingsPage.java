package com.shade.decima.ui.settings;

import com.shade.decima.ui.Application;
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
import java.util.prefs.Preferences;

@SettingsPageRegistration(id = "wwise", name = "Wwise Audio")
public class WwiseSettingsPage implements SettingsPage {
    public static final String PROP_WW2OGG_PATH = "ww2oggPath";
    public static final String PROP_WW2OGG_CODEBOOKS_PATH = "ww2oggCodebooksPath";
    public static final String PROP_REVORB_PATH = "revorbPath";
    public static final String PROP_FFMPEG_PATH = "ffmpegPath";

    private JTextField ww2oggPath;
    private JTextField ww2oggCodebooksPath;
    private JTextField revorbPath;
    private JTextField ffmpegPath;

    @NotNull
    public static Preferences getPreferences() {
        return Application.getWorkspace().getPreferences().node("settings/wwise");
    }

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
        final Preferences pref = getPreferences();
        pref.put(PROP_WW2OGG_PATH, ww2oggPath.getText());
        pref.put(PROP_WW2OGG_CODEBOOKS_PATH, ww2oggCodebooksPath.getText());
        pref.put(PROP_REVORB_PATH, revorbPath.getText());
        pref.put(PROP_FFMPEG_PATH, ffmpegPath.getText());
    }

    @Override
    public void reset() {
        final Preferences pref = getPreferences();
        ww2oggPath.setText(pref.get(PROP_WW2OGG_PATH, ""));
        ww2oggCodebooksPath.setText(pref.get(PROP_WW2OGG_CODEBOOKS_PATH, ""));
        revorbPath.setText(pref.get(PROP_REVORB_PATH, ""));
        ffmpegPath.setText(pref.get(PROP_FFMPEG_PATH, ""));
    }

    @Override
    public boolean isModified() {
        final Preferences pref = getPreferences();
        return !ww2oggPath.getText().equals(pref.get(PROP_WW2OGG_PATH, ""))
            || !ww2oggCodebooksPath.getText().equals(pref.get(PROP_WW2OGG_CODEBOOKS_PATH, ""))
            || !revorbPath.getText().equals(pref.get(PROP_REVORB_PATH, ""))
            || !ffmpegPath.getText().equals(pref.get(PROP_FFMPEG_PATH, ""));
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(ww2oggPath)
            && UIUtils.isValid(ww2oggCodebooksPath)
            && UIUtils.isValid(revorbPath)
            && UIUtils.isValid(ffmpegPath);
    }
}
