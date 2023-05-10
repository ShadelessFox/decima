package com.shade.decima.ui.settings;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.platform.ui.settings.SettingsKey;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

@SettingsPageRegistration(parent = "coreEditor", id = "wwise", name = "Wwise Audio")
public class WwiseSettingsPage implements SettingsPage {
    public static final SettingsKey<String> WW2OGG_PATH = SettingsKey.of("ww2oggPath", "");
    public static final SettingsKey<String> WW2OGG_CODEBOOKS_PATH = SettingsKey.of("ww2oggCodebooksPath", "");
    public static final SettingsKey<String> REVORB_PATH = SettingsKey.of("revorbPath", "");
    public static final SettingsKey<String> FFMPEG_PATH = SettingsKey.of("ffmpegPath", "");

    private JTextField ww2oggPath;
    private JTextField ww2oggCodebooksPath;
    private JTextField revorbPath;
    private JTextField ffmpegPath;

    @NotNull
    public static Preferences getPreferences() {
        return Application.getPreferences().node("settings/wwise");
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
        WW2OGG_PATH.set(pref, ww2oggPath.getText());
        WW2OGG_CODEBOOKS_PATH.set(pref, ww2oggCodebooksPath.getText());
        REVORB_PATH.set(pref, revorbPath.getText());
        FFMPEG_PATH.set(pref, ffmpegPath.getText());
    }

    @Override
    public void reset() {
        final Preferences pref = getPreferences();
        ww2oggPath.setText(WW2OGG_PATH.get(pref));
        ww2oggCodebooksPath.setText(WW2OGG_CODEBOOKS_PATH.get(pref));
        revorbPath.setText(REVORB_PATH.get(pref));
        ffmpegPath.setText(FFMPEG_PATH.get(pref));
    }

    @Override
    public boolean isModified() {
        final Preferences pref = getPreferences();
        return !WW2OGG_PATH.get(pref).equals(ww2oggPath.getText())
            || !WW2OGG_CODEBOOKS_PATH.get(pref).equals(ww2oggCodebooksPath.getText())
            || !REVORB_PATH.get(pref).equals(revorbPath.getText())
            || !FFMPEG_PATH.get(pref).equals(ffmpegPath.getText());
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(ww2oggPath)
            && UIUtils.isValid(ww2oggCodebooksPath)
            && UIUtils.isValid(revorbPath)
            && UIUtils.isValid(ffmpegPath);
    }
}
