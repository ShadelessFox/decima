package com.shade.decima.ui.updater;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.build.BuildConfig;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;

class UpdateInfoDialog extends BaseDialog {
    private static final ButtonDescriptor BUTTON_BROWSE = new ButtonDescriptor("ok", "Browse", null);
    private static final ButtonDescriptor BUTTON_IGNORE = new ButtonDescriptor("ignore", "Ignore this update", null);

    private final UpdateService.UpdateInfo info;

    UpdateInfoDialog(@NotNull UpdateService.UpdateInfo info) {
        super("Update is available", true);
        this.info = info;
    }

    @NotNull
    @Override
    protected JComponent createContentsPane() {
        JLabel title = new JLabel("New update is available");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

        JLabel description = new JLabel(MessageFormat.format(
            "{0} ({1,date,short}) -> {2} ({3,date,short})",
            BuildConfig.APP_VERSION,
            BuildConfig.BUILD_TIME,
            info.version(),
            info.publishedAt().toEpochSecond() * 1000
        ));

        JPanel header = new JPanel();
        header.setLayout(new MigLayout("ins dialog,wrap", "[grow,fill]", "[][][grow,fill]"));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIColor.SHADOW));
        header.setBackground(UIColor.named("Dialog.buttonBackground"));
        header.add(title);
        header.add(description);

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet ss = kit.getStyleSheet();
        ss.addRule("code { font-size: inherit; font-family: Monospaced; }");
        ss.addRule("ul { margin-left-ltr: 16px; margin-right-ltr: 16px; }");
        ss.addRule("h1 { font-size: 2em; }");
        ss.addRule("h2 { font-size: 1.5em; }");
        ss.addRule("h3 { font-size: 1.25em; }");
        ss.addRule("h4 { font-size: 1em; }");

        JEditorPane changelog = UIUtils.createBrowseText(info.contents());
        changelog.setPreferredSize(new Dimension(800, 500));

        JPanel center = new JPanel();
        center.setLayout(new MigLayout("ins dialog"));
        center.add(UIUtils.createBorderlessScrollPane(changelog));

        JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    @Override
    protected void buttonPressed(@NotNull ButtonDescriptor descriptor) {
        if (descriptor.equals(BUTTON_BROWSE)) {
            try {
                Desktop.getDesktop().browse(info.externalUri());
            } catch (IOException e) {
                UIUtils.showErrorDialog(e, "Unable to open the release page in the browser");
            }
        } else if (descriptor.equals(BUTTON_IGNORE)) {
            UpdateService.getInstance().ignoreUpdate(info.version());
        }

        super.buttonPressed(descriptor);
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getLeftButtons() {
        return new ButtonDescriptor[]{BUTTON_IGNORE};
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{BUTTON_BROWSE, BUTTON_CANCEL};
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_BROWSE;
    }
}
