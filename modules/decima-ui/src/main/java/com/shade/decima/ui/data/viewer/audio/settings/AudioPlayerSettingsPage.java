package com.shade.decima.ui.data.viewer.audio.settings;

import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.controls.validators.ExistingFileValidator;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.settings.SettingsPage;
import com.shade.platform.ui.settings.SettingsPageRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Objects;

@SettingsPageRegistration(parent = "coreEditor", id = "wwise", name = "Audio Player")
public class AudioPlayerSettingsPage implements SettingsPage {
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
        tools.setLayout(new MigLayout("ins panel,wrap", "[fill][grow,fill,400lp]", ""));

        tools.add(new JLabel("ww2ogg executable:"));
        tools.add(ww2oggPath);

        tools.add(new JLabel("ww2ogg codebooks:"));
        tools.add(ww2oggCodebooksPath);

        tools.add(new JLabel("revorb executable:"));
        tools.add(revorbPath);

        tools.add(new JLabel("ffmpeg executable:"));
        tools.add(ffmpegPath);

        final JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setText("You can download individual entries from the following links: <a href=\"https://github.com/hcs64/ww2ogg\">ww2ogg</a>, <a href=\"https://hydrogenaud.io/index.php/topic,64328.0.html\">revorb</a>, and <a href=\"https://ffmpeg.org/\">ffmpeg</a>.");
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                IOUtils.unchecked(() -> {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                    return null;
                });
            }
        });

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel(UIManager.getIcon("Action.informationIcon")));
        panel.add(pane);

        tools.add(panel, "span");

        return tools;
    }

    @Override
    public void apply() {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        settings.ww2oggPath = IOUtils.getTrimmedOrNullIfEmpty(ww2oggPath.getText());
        settings.ww2oggCodebooksPath = IOUtils.getTrimmedOrNullIfEmpty(ww2oggCodebooksPath.getText());
        settings.revorbPath = IOUtils.getTrimmedOrNullIfEmpty(revorbPath.getText());
        settings.ffmpegPath = IOUtils.getTrimmedOrNullIfEmpty(ffmpegPath.getText());
    }

    @Override
    public void reset() {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        ww2oggPath.setText(settings.ww2oggPath);
        ww2oggCodebooksPath.setText(settings.ww2oggCodebooksPath);
        revorbPath.setText(settings.revorbPath);
        ffmpegPath.setText(settings.ffmpegPath);
    }

    @Override
    public boolean isModified() {
        final AudioPlayerSettings settings = AudioPlayerSettings.getInstance();
        return !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(ww2oggPath.getText()), settings.ww2oggPath)
            || !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(ww2oggCodebooksPath.getText()), settings.ww2oggCodebooksPath)
            || !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(revorbPath.getText()), settings.revorbPath)
            || !Objects.equals(IOUtils.getTrimmedOrNullIfEmpty(ffmpegPath.getText()), settings.ffmpegPath);
    }

    @Override
    public boolean isComplete() {
        return UIUtils.isValid(ww2oggPath)
            && UIUtils.isValid(ww2oggCodebooksPath)
            && UIUtils.isValid(revorbPath)
            && UIUtils.isValid(ffmpegPath);
    }
}
