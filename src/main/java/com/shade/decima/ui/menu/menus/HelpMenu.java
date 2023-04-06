package com.shade.decima.ui.menu.menus;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shade.decima.BuildConfig;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.html.HtmlEditorInput;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.menus.Menu;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuRegistration(id = APP_MENU_HELP_ID, name = "&Help", order = 4000)
public final class HelpMenu extends Menu {
    @MenuItemRegistration(parent = APP_MENU_HELP_ID, name = "&Help", keystroke = "F1", group = APP_MENU_HELP_GROUP_HELP, order = 1000)
    public static class HelpItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/ShadelessFox/decima/wiki"));
            } catch (IOException e) {
                UIUtils.showErrorDialog(Application.getFrame(), e, "Unable to open wiki page");
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_HELP_ID, name = "&Changelog", group = APP_MENU_HELP_GROUP_ABOUT, order = 1000)
    public static class ChangelogItem extends MenuItem {
        private static final Pattern COMMIT_PATTERN = Pattern.compile("([a-fA-F0-9]{40})");
        private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
        private static final Gson GSON = new Gson();

        public static void open() {
            Application.getEditorManager().openEditor(new ChangelogEditorInputLazy(true), null, null, true, true, 0);
        }

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            open();
        }

        private static class ChangelogEditorInput extends HtmlEditorInput {
            public ChangelogEditorInput(@NotNull String body) {
                super("Changelog", body);
            }
        }

        private static record ChangelogEditorInputLazy(boolean canLoadImmediately) implements LazyEditorInput {
            @NotNull
            @Override
            public EditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
                final var client = HttpClient.newHttpClient();
                final var release = client.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/ShadelessFox/decima/releases/tags/v%s".formatted(
                            BuildConfig.APP_VERSION
                        )))
                        .build(),
                    info -> BodySubscribers.<String, Map<String, Object>>mapping(
                        BodySubscribers.ofString(StandardCharsets.UTF_8),
                        body -> GSON.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType())
                    )
                );
                final var markdown = client.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/markdown"))
                        .POST(BodyPublishers.ofString(GSON.toJson(Map.of(
                            "text", release.body().get("body")
                        ))))
                        .build(),
                    info -> BodySubscribers.mapping(
                        BodySubscribers.ofString(StandardCharsets.UTF_8),
                        body -> {
                            body = COMMIT_PATTERN
                                .matcher(body)
                                .replaceAll(result -> "<code><a href=\"https://github.com/ShadelessFox/decima/commit/%s\">%s</a></code>".formatted(
                                    result.group(1),
                                    result.group(1).substring(0, 7)
                                ));
                            body = MENTION_PATTERN
                                .matcher(body)
                                .replaceAll("<a href=\"https://github.com/$1\">$0</a>");
                            return "<h1>What's new in %s %s</h1>%s".formatted(
                                BuildConfig.APP_TITLE,
                                BuildConfig.APP_VERSION,
                                body
                            );
                        }
                    )
                );
                return new ChangelogEditorInput(markdown.body());
            }

            @NotNull
            @Override
            public String getName() {
                return "Changelog";
            }

            @Nullable
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public boolean representsSameResource(@NotNull EditorInput other) {
                return other instanceof ChangelogEditorInput || other instanceof ChangelogEditorInputLazy;
            }

            @NotNull
            @Override
            public LazyEditorInput canLoadImmediately(boolean value) {
                return new ChangelogEditorInputLazy(value);
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_HELP_ID, name = "&About", group = APP_MENU_HELP_GROUP_ABOUT, order = 2000)
    public static class AboutItem extends MenuItem {
        private static final MessageFormat MESSAGE = new MessageFormat("""
            <h1>{0}</h1>
            A tool for viewing and editing data in games powered by Decima engine.
            <br><br>
            <table>
            <tr><td><b>Version:</b></td><td>{1} (Built on {2,date,short}), commit: <a href="https://github.com/ShadelessFox/decima/commit/{3}">{3}</a></tr>
            <tr><td><b>VM Version:</b></td><td>{4}; {5} ({6} {7})</td></tr>
            <tr><td><b>VM Vendor:</b></td><td>{8}, <a href="{9}">{9}</a></td></tr>
            </table>
            <br>
            See <a href="https://github.com/ShadelessFox/decima">https://github.com/ShadelessFox/decima</a> for more information.
            """);

        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Properties p = System.getProperties();

            final JEditorPane pane = new JEditorPane();
            pane.setEditorKit(new HTMLEditorKit());
            pane.setEditable(false);
            pane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    IOUtils.unchecked(() -> {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                        return null;
                    });
                }
            });
            pane.setText(MESSAGE.format(new Object[]{
                BuildConfig.APP_TITLE,
                BuildConfig.APP_VERSION, BuildConfig.BUILD_TIME, BuildConfig.BUILD_COMMIT,
                p.get("java.version"), p.get("java.vm.name"), p.get("java.vm.version"), p.get("java.vm.info"),
                p.get("java.vendor"), p.get("java.vendor.url")
            }));

            JOptionPane.showMessageDialog(
                Application.getFrame(),
                pane,
                "About",
                JOptionPane.PLAIN_MESSAGE
            );

        }
    }
}
