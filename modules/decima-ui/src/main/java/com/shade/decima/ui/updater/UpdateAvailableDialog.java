package com.shade.decima.ui.updater;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shade.decima.BuildConfig;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

public class UpdateAvailableDialog extends BaseDialog {
    private static final String REPOSITORY_NAME = "ShadelessFox/decima";

    private final UpdateInfo info;

    private UpdateAvailableDialog(@NotNull UpdateInfo info) {
        super("Update is available", true);
        this.info = info;
    }

    public static void checkForUpdates() {
        Optional<UpdateInfo> result;

        try {
            result = ProgressDialog.showProgressDialog(
                JOptionPane.getRootFrame(),
                "Checking for updates",
                UpdateAvailableDialog::checkForUpdates
            );
        } catch (Exception e) {
            UIUtils.showErrorDialog(e, "Unable to check for updates");
            return;
        }

        if (result.isEmpty()) {
            JOptionPane.showMessageDialog(
                JOptionPane.getRootFrame(),
                "You are using the latest version of " + BuildConfig.APP_TITLE + ".",
                "No updates available",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        UpdateInfo info = result.get();
        if (new UpdateAvailableDialog(info).showDialog(JOptionPane.getRootFrame()) != BUTTON_BROWSE) {
            return;
        }

        try {
            Desktop.getDesktop().browse(info.uri);
        } catch (IOException e) {
            UIUtils.showErrorDialog(e, "Unable to open the release page in the browser");
        }
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
            info.version,
            info.publishedAt.toEpochSecond() * 1000
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

        JEditorPane editor = new JEditorPane();
        editor.setEditorKit(kit);
        editor.setEditable(false);
        editor.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                IOUtils.unchecked(() -> {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                    return null;
                });
            }
        });
        editor.setText(info.body);
        editor.setPreferredSize(new Dimension(800, 500));

        JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(UIUtils.createBorderlessScrollPane(editor), BorderLayout.CENTER);

        return root;
    }

    @NotNull
    @Override
    protected ButtonDescriptor[] getButtons() {
        return new ButtonDescriptor[]{
            BUTTON_BROWSE,
            BUTTON_CANCEL
        };
    }

    @Nullable
    @Override
    protected ButtonDescriptor getDefaultButton() {
        return BUTTON_BROWSE;
    }

    @Nullable
    private static UpdateInfo checkForUpdates(@NotNull ProgressMonitor monitor) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        JsonObject release;
        try (var ignored = monitor.begin("Checking for updates")) {
            release = fetchLatestRelease(client);
        }

        String latestTag = release.get("tag_name").getAsString();
        String currentTag = "v" + BuildConfig.APP_VERSION;
        if (latestTag.equals(currentTag)) {
            return null;
        }

        return new UpdateInfo(
            latestTag,
            convertMarkdownToHtml(client, release.get("body").getAsString()),
            ZonedDateTime.parse(release.get("published_at").getAsString()),
            URI.create(release.get("html_url").getAsString())
        );
    }

    @NotNull
    private static JsonObject fetchLatestRelease(@NotNull HttpClient client) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com" + "/repos/" + REPOSITORY_NAME + "/releases/latest"))
                .build(),
            info1 -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                body -> JsonParser.parseString(body).getAsJsonObject()
            )
        ).body();
    }

    @NotNull
    private static String convertMarkdownToHtml(@NotNull HttpClient client, @NotNull String markdown) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com" + "/markdown"))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(Map.of(
                    "text", markdown,
                    "mode", "gfm",
                    "context", REPOSITORY_NAME
                ))))
                .build(),
            info -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                UpdateAvailableDialog::convertMentions
            )
        ).body();
    }

    @NotNull
    private static String convertMentions(@NotNull String body) {
        return body.replaceAll("@(\\w+)", "<a href=\"https://github.com/$1\">$0</a>");
    }

    private record UpdateInfo(
        @NotNull String version,
        @NotNull String body,
        @NotNull ZonedDateTime publishedAt,
        @NotNull URI uri
    ) {}
}
