package com.shade.decima.ui.updater;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import com.shade.decima.model.util.LocalDateTimeAdapter;
import com.shade.decima.ui.Application;
import com.shade.platform.model.Service;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.persistence.PersistableComponent;
import com.shade.platform.model.persistence.Persistent;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Service(UpdateService.class)
@Persistent("UpdateService")
public class UpdateService implements PersistableComponent<UpdateService.Settings> {
    public static class Settings {
        public final Set<String> ignoredUpdates = new LinkedHashSet<>();

        @JsonAdapter(LocalDateTimeAdapter.class)
        public LocalDateTime lastCheck = null;

        public boolean checkForUpdates = true;
    }

    private static final Logger log = getLogger(UpdateService.class);
    private static final HttpClient client = HttpClient.newHttpClient();

    private static final String REPOSITORY_NAME = "ShadelessFox/decima";
    private static final long CHECK_PERIOD = Duration.ofDays(1).toMinutes();

    private Settings settings = new Settings();
    private boolean currentlyShowingDialog = false;

    @NotNull
    public static UpdateService getInstance() {
        return ApplicationManager.getApplication().getService(UpdateService.class);
    }

    public void checkForUpdatesModal(@Nullable Window owner) {
        Optional<UpdateInfo> result;

        try {
            result = ProgressDialog.showProgressDialog(
                owner,
                "Checking for updates",
                this::fetchUpdateInfo
            );
        } catch (Exception e) {
            UIUtils.showErrorDialog(e, "Unable to check for updates");
            return;
        }

        showUpdateInfo(owner, result.orElse(null));
    }

    public void schedule() {
        long elapsed = settings.lastCheck != null ? Duration.between(settings.lastCheck, LocalDateTime.now()).toMinutes() : 0;
        Executors
            .newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::checkForUpdatesBackground, Math.max(0, CHECK_PERIOD - elapsed), CHECK_PERIOD, TimeUnit.MINUTES);
    }

    public void ignoreUpdate(@NotNull String version) {
        settings.ignoredUpdates.add(version);
    }

    @Nullable
    @Override
    public Settings getState() {
        return settings;
    }

    @Override
    public void loadState(@NotNull Settings state) {
        settings = state;
    }

    @NotNull
    public Settings getSettings() {
        return settings;
    }

    private void checkForUpdatesBackground() {
        if (!settings.checkForUpdates || currentlyShowingDialog) {
            return;
        }
        log.debug("Checking for updates");
        UpdateInfo info = null;
        try {
            info = fetchUpdateInfo(new VoidProgressMonitor());
        } catch (Exception e) {
            log.error("Unable to check for updates", e);
        }
        if (info == null) {
            log.debug("No updates available");
            return;
        }
        log.info("New update is available: {}", info.version());
        UpdateInfo finalInfo = info;
        SwingUtilities.invokeLater(() -> {
            currentlyShowingDialog = true;
            showUpdateInfo(JOptionPane.getRootFrame(), finalInfo);
            currentlyShowingDialog = false;
        });
    }

    private static void showUpdateInfo(@Nullable Window owner, @Nullable UpdateInfo info) {
        if (info == null) {
            JOptionPane.showMessageDialog(
                owner,
                "You are using the latest version of " + Application.getInstance().getTitle() + ".",
                "No updates available",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            new UpdateInfoDialog(info).showDialog(owner);
        }
    }

    @Nullable
    private UpdateInfo fetchUpdateInfo(@NotNull ProgressMonitor monitor) throws Exception {
        settings.lastCheck = LocalDateTime.now();

        JsonObject release;
        try (ProgressMonitor.IndeterminateTask ignored = monitor.begin("Checking for updates")) {
            release = fetchLatestRelease();
        }

        String latestTag = release.get("tag_name").getAsString();
        if (latestTag.equals(getCurrentTag())) {
            // No updates available
            return null;
        }
        if (settings.ignoredUpdates.contains(latestTag)) {
            // This update was ignored
            return null;
        }

        return new UpdateInfo(
            latestTag,
            convertMarkdownToHtml(release.get("body").getAsString()),
            ZonedDateTime.parse(release.get("published_at").getAsString()),
            URI.create(release.get("html_url").getAsString())
        );
    }

    @NotNull
    private static JsonObject fetchLatestRelease() throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com" + "/repos/" + REPOSITORY_NAME + "/releases/latest"))
                .build(),
            info -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                body -> JsonParser.parseString(body).getAsJsonObject()
            )
        ).body();
    }

    @NotNull
    private static String convertMarkdownToHtml(@NotNull String markdown) throws IOException, InterruptedException {
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
                UpdateService::convertMentions
            )
        ).body();
    }

    @NotNull
    private static String convertMentions(@NotNull String body) {
        return body.replaceAll("@(\\w+)", "<a href=\"https://github.com/$1\">$0</a>");
    }

    @NotNull
    private static String getCurrentTag() {
        return "v" + Application.getInstance().getVersion();
    }

    record UpdateInfo(
        @NotNull String version,
        @NotNull String contents,
        @NotNull ZonedDateTime publishedAt,
        @NotNull URI externalUri
    ) {}
}
