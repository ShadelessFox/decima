package com.shade.decima.ui;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.util.SystemInfo;
import com.shade.decima.model.build.BuildConfig;
import com.shade.decima.cli.ApplicationCLI;
import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.ui.editor.NodeEditorInputLazy;
import com.shade.decima.ui.editor.ProjectEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.decima.ui.navigator.NavigatorView;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.menu.ProjectCloseItem;
import com.shade.decima.ui.updater.UpdateService;
import com.shade.platform.model.ElementFactory;
import com.shade.platform.model.ExtensionRegistry;
import com.shade.platform.model.Lazy;
import com.shade.platform.model.ServiceManager;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformMenuConstants;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.controls.MemoryIndicator;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorChangeListener;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.editors.lazy.UnloadableEditorInput;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.views.ViewManager;
import com.shade.platform.ui.wm.StatusBar;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class Application implements com.shade.platform.model.app.Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Preferences preferences;
    private ServiceManager serviceManager;

    private MessageBusConnection connection;
    private JFrame frame;

    static {
        configureLogger();
    }

    @NotNull
    public static Application getInstance() {
        return (Application) ApplicationManager.getApplication();
    }

    @Override
    public void start(@NotNull String[] args) {
        final Properties p = System.getProperties();

        log.info("Starting {} ({}, {})", BuildConfig.APP_TITLE, BuildConfig.APP_VERSION, BuildConfig.BUILD_COMMIT);
        log.info("--- Information ---");
        log.info("OS: {} ({}, {})", p.get("os.name"), p.get("os.version"), p.get("os.arch"));
        log.info("VM Version: {}; {} ({} {})", p.get("java.version"), p.get("java.vm.name"), p.get("java.vm.version"), p.get("java.vm.info"));
        log.info("VM Vendor: {}, {}", p.get("java.vendor"), p.get("java.vendor.url"));
        log.info("VM Arguments: {}", ManagementFactory.getRuntimeMXBean().getInputArguments());
        log.info("CLI Arguments: {}", Arrays.asList(args));
        log.info("-------------------");

        if (args.length == 0) {
            Splash.getInstance().show();
        }

        Splash.getInstance().update("Loading services and configuration", 0.0f);

        this.preferences = Preferences.userRoot().node("decima-explorer");
        this.serviceManager = new ServiceManager(getConfigPath());

        if (args.length > 0) {
            ApplicationCLI.execute(args);
            return;
        }

        Splash.getInstance().show();

        connection = MessageBus.getInstance().connect();

        Splash.getInstance().update("Configuring user interface", 0.5f);

        configureUI();
        frame = new JFrame();
        configureFrame(frame);

        MenuManager.getInstance().installMenuBar(frame.getRootPane(), PlatformMenuConstants.APP_MENU_ID, key -> {
            final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

            for (Component cur = manager.getPermanentFocusOwner(); cur instanceof JComponent c; cur = cur.getParent()) {
                final DataContext context = (DataContext) c.getClientProperty(MenuManager.CONTEXT_KEY);

                if (context != null) {
                    final Object data = context.getData(key);

                    if (data != null) {
                        return data;
                    }
                }
            }

            return null;
        });

        connection.subscribe(MenuManager.SELECTION, new MenuSelectionListener() {
            @Override
            public void selectionChanged(@NotNull MenuItem item, @NotNull MenuItemRegistration registration, @Nullable MenuItemContext context) {
                StatusBar.set(registration.description());
            }

            @Override
            public void selectionCleared() {
                StatusBar.set(null);
            }
        });

        final StatusBarImpl statusBar = new StatusBarImpl();
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIColor.SHADOW));
        connection.subscribe(StatusBar.TOPIC, statusBar);

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(ViewManager.getInstance().getComponent(), BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);

        Splash.getInstance().update("Done", 1.0f);

        frame.setTitle(getApplicationTitle());
        frame.setIconImages(FlatSVGUtils.createWindowIconImages(getClass().getResource("/icons/application.svg")));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setVisible(true);

        Splash.getInstance().hide();
        UpdateService.getInstance().schedule();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                serviceManager.persist();
            } catch (IOException e) {
                log.warn("Error during periodical state save", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public <T> T getService(@NotNull Class<T> cls) {
        return serviceManager.getService(cls);
    }

    @Nullable
    @Override
    public ElementFactory getElementFactory(@NotNull String id) {
        return ExtensionRegistry.getExtensions(ElementFactory.class, ElementFactory.Registration.class).stream()
            .filter(factory -> factory.metadata().value().equals(id))
            .findFirst().map(Lazy::get).orElse(null);
    }

    @NotNull
    public static NavigatorTree getNavigator() {
        return Objects.requireNonNull(ViewManager.getInstance().<NavigatorView>findView(NavigatorView.ID)).getTree();
    }

    @NotNull
    private static String getApplicationTitle() {
        final Editor activeEditor = EditorManager.getInstance().getActiveEditor();
        if (activeEditor != null) {
            return BuildConfig.APP_TITLE + " - " + activeEditor.getInput().getName();
        } else {
            return BuildConfig.APP_TITLE;
        }
    }

    private void configureFrame(@NotNull JFrame frame) {
        try {
            restoreWindow(preferences.node("window"));
        } catch (Exception e) {
            log.warn("Unable to restore window visuals", e);
        }

        JOptionPane.setRootFrame(frame);

        connection.subscribe(ApplicationSettings.SETTINGS, new ApplicationSettingsChangeListener() {
            @Override
            public void fontChanged(@Nullable String fontFamily, int fontSize) {
                if (fontFamily == null) {
                    UIManager.put("defaultFont", null);
                } else {
                    UIManager.put("defaultFont", StyleContext.getDefaultStyleContext().getFont(fontFamily, Font.PLAIN, fontSize));
                }

                FlatLaf.updateUI();
            }

            @Override
            public void themeChanged(@Nullable String themeClassName) {
                try {
                    UIManager.setLookAndFeel(themeClassName);
                } catch (Exception e) {
                    log.error("Failed to setup look and feel '" + themeClassName + "': " + e);
                }

                FlatLaf.updateUI();
            }
        });
        connection.subscribe(EditorManager.EDITORS, new EditorChangeListener() {
            @Override
            public void editorChanged(@Nullable Editor editor) {
                frame.setTitle(getApplicationTitle());
            }
        });
        connection.subscribe(ProjectManager.PROJECTS, new ProjectChangeListener() {
            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                final EditorManager manager = EditorManager.getInstance();

                for (Editor editor : manager.getEditors()) {
                    final EditorInput input = editor.getInput();

                    if (isSameProject(input, container)) {
                        manager.closeEditor(editor);
                    }
                }
            }

            @Override
            public void projectOpened(@NotNull ProjectContainer container) {
                final EditorManager manager = EditorManager.getInstance();

                for (Editor editor : manager.getEditors()) {
                    final EditorInput input = editor.getInput();

                    if (input instanceof LazyEditorInput i && !i.canLoadImmediately() && isSameProject(i, container)) {
                        manager.reuseEditor(editor, i.canLoadImmediately(true));
                    }
                }
            }

            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                final EditorManager manager = EditorManager.getInstance();

                for (Editor editor : manager.getEditors()) {
                    final EditorInput input = editor.getInput();

                    if (isSameProject(input, container)) {
                        if (input instanceof UnloadableEditorInput uei) {
                            manager.reuseEditor(editor, uei.unloadInput());
                        } else {
                            manager.closeEditor(editor);
                        }
                    }
                }
            }

            private static boolean isSameProject(@NotNull EditorInput input, @NotNull ProjectContainer container) {
                return input instanceof ProjectEditorInput pei && pei.getProject().getContainer().equals(container)
                    || input instanceof NodeEditorInputLazy nei && nei.container().equals(container.getId());
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                final NavigatorTreeModel model = Application.getNavigator().getModel();

                for (ProjectContainer container : ProjectManager.getInstance().getProjects()) {
                    final NavigatorProjectNode node = model.getProjectNode(new VoidProgressMonitor(), container);

                    if (node.isOpen() && !ProjectCloseItem.confirmProjectClose(node.getProject(), EditorManager.getInstance())) {
                        return;
                    }
                }

                saveState();
                System.exit(0);
            }
        });

        try {
            restoreWindow(preferences.node("window"));
        } catch (Exception e) {
            log.warn("Unable to restore window visuals", e);
        }
    }

    private void configureUI() {
        final ApplicationSettings settings = ApplicationSettings.getInstance();

        if (settings.customFontFamily != null) {
            UIManager.put("defaultFont", StyleContext.getDefaultStyleContext().getFont(settings.customFontFamily, Font.PLAIN, settings.customFontSize));
        }

        FlatLaf.registerCustomDefaultsSource(getClass().getResource("/themes/"));
        FlatInspector.install("ctrl shift alt X");
        FlatUIDefaultsInspector.install("ctrl shift alt Y");

        try {
            UIManager.setLookAndFeel(settings.themeClassName);
        } catch (Exception e) {
            log.error("Failed to setup look and feel '" + settings.themeClassName + "'l: " + e);
        }

        List<String> icons = List.of(
            "Action.addElementIcon", "actions/add_element.svg",
            "Action.closeAllIcon", "actions/tab_close_all.svg",
            "Action.closeIcon", "actions/tab_close.svg",
            "Action.closeOthersIcon", "actions/tab_close_others.svg",
            "Action.closeUninitializedIcon", "actions/tab_close_uninitialized.svg",
            "Action.containsIcon", "actions/contains.svg",
            "Action.copyIcon", "actions/copy.svg",
            "Action.duplicateElementIcon", "actions/duplicate_element.svg",
            "Action.editIcon", "actions/edit.svg",
            "Action.editModalIcon", "actions/edit_modal.svg",
            "Action.exportIcon", "actions/export.svg",
            "Action.hideIcon", "actions/hide.svg",
            "Action.importIcon", "actions/import.svg",
            "Action.informationIcon", "actions/information.svg",
            "Action.navigateIcon", "actions/navigate.svg",
            "Action.nextIcon", "actions/next.svg",
            "Action.normalsIcon", "actions/normals.svg",
            "Action.nullTerminatorIcon", "actions/null_terminator.svg",
            "Action.outlineIcon", "actions/outline.svg",
            "Action.packIcon", "actions/pack.svg",
            "Action.pauseIcon", "actions/pause.svg",
            "Action.playIcon", "actions/play.svg",
            "Action.previousIcon", "actions/previous.svg",
            "Action.questionIcon", "actions/question.svg",
            "Action.redoIcon", "actions/redo.svg",
            "Action.refreshIcon", "actions/refresh.svg",
            "Action.removeElementIcon", "actions/remove_element.svg",
            "Action.saveIcon", "actions/save.svg",
            "Action.searchIcon", "actions/search.svg",
            "Action.shadingIcon", "actions/shading.svg",
            "Action.splitDownIcon", "actions/split_down.svg",
            "Action.splitRightIcon", "actions/split_right.svg",
            "Action.starIcon", "actions/star.svg",
            "Action.lowerCaseIcon", "actions/lowercase.svg",
            "Action.upperCaseIcon", "actions/uppercase.svg",
            "Action.undoIcon", "actions/undo.svg",
            "Action.wireframeIcon", "actions/wireframe.svg",
            "Action.zoomFitIcon", "actions/zoom_fit.svg",
            "Action.zoomInIcon", "actions/zoom_in.svg",
            "Action.zoomOutIcon", "actions/zoom_out.svg",
            "File.binaryIcon", "files/binary.svg",
            "File.coreIcon", "files/core.svg",
            "Node.archiveIcon", "nodes/archive.svg",
            "Node.arrayIcon", "nodes/array.svg",
            "Node.booleanIcon", "nodes/boolean.svg",
            "Node.decimalIcon", "nodes/decimal.svg",
            "Node.enumIcon", "nodes/enum.svg",
            "Node.integerIcon", "nodes/integer.svg",
            "Node.modelIcon", "nodes/model.svg",
            "Node.monitorActiveIcon", "nodes/monitorActive.svg",
            "Node.monitorInactiveIcon", "nodes/monitorInactive.svg",
            "Node.objectIcon", "nodes/object.svg",
            "Node.referenceIcon", "nodes/reference.svg",
            "Node.stringIcon", "nodes/string.svg",
            "Node.textureIcon", "nodes/texture.svg",
            "Node.uuidIcon", "nodes/uuid.svg",
            "Overlay.addIcon", "overlays/add.svg",
            "Overlay.modifyIcon", "overlays/modify.svg",
            "Tree.closedIcon", "nodes/folder.svg",
            "Tree.leafIcon", "nodes/file.svg",
            "Tree.openIcon", "nodes/folder.svg"
        );

        for (int i = 0; i < icons.size(); i += 2) {
            var key = icons.get(i);
            var url = getClass().getResource("/icons/%s".formatted(icons.get(i + 1)));
            if (url == null) {
                log.error("Unable to load icon for key '{}'", key);
                continue;
            }
            UIManager.put(key, new FlatSVGIcon(url));
        }

        // See resources/icons/guidelines.md for more information
        final FlatSVGIcon.ColorFilter filter = FlatSVGIcon.ColorFilter.getInstance();
        filter.add(new Color(0x6C707E), UIColor.named("Icon.baseColor"));
        filter.add(new Color(0xEBECF0), UIColor.named("Icon.baseColor2"));
        filter.add(new Color(0x3574F0), UIColor.named("Icon.accentColor"));
        filter.add(new Color(0xE7EFFD), UIColor.named("Icon.accentColor2"));
    }

    private void saveState() {
        serviceManager.dispose();

        try {
            saveWindow(preferences);
        } catch (Exception e) {
            log.warn("Unable to save window visuals", e);
        }
    }

    private void saveWindow(@NotNull Preferences pref) {
        final Preferences node = pref.node("window");
        node.putLong("size", (long) frame.getWidth() << 32 | frame.getHeight());
        node.putLong("location", (long) frame.getX() << 32 | frame.getY());
        node.putBoolean("maximized", (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) > 0);
    }

    private void restoreWindow(@NotNull Preferences pref) {
        final var size = pref.getLong("size", 0);
        final var location = pref.getLong("location", 0);
        final var maximized = pref.getBoolean("maximized", false);

        if (size > 0 && location >= 0) {
            frame.setSize((int) (size >>> 32), (int) size);
            frame.setLocation((int) (location >>> 32), (int) location);
        } else {
            frame.setSize(1280, 720);
            frame.setLocationRelativeTo(null);
        }

        if (maximized) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private static void configureLogger() {
        final var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final var logger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        final var encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n");
        encoder.start();

        final var appender = new FileAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setName("logFile");
        appender.setFile(getWorkspacePath().resolve("decima-workshop.log").toString());
        appender.setEncoder(encoder);
        appender.setAppend(false);
        appender.start();

        logger.addAppender(appender);
    }

    @NotNull
    private static Path getWorkspacePath() {
        final String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new IllegalStateException("Unable to determine user home directory");
        }
        if (SystemInfo.isWindows) {
            return Path.of(userHome, "AppData", "Local", "DecimaWorkshop");
        } else if (SystemInfo.isMacOS) {
            return Path.of(userHome, "Library", "Application Support", "DecimaWorkshop");
        } else {
            return Path.of(userHome, ".config", "decima-workshop");
        }
    }

    @NotNull
    private static Path getConfigPath() {
        final Path legacyPath = Path.of("config", "workspace.json").toAbsolutePath();
        final Path modernPath = getWorkspacePath().resolve("config.json");

        // Before 0.1.20, the config file was located next to the executable
        if (Files.exists(legacyPath) && Files.notExists(modernPath)) {
            log.info("Migrating config file from '{}' to '{}'", legacyPath, modernPath);

            try {
                Files.createDirectories(modernPath.getParent());
                Files.move(legacyPath, modernPath);
                Files.delete(legacyPath.getParent());
            } catch (IOException e) {
                log.error("Unable to migrate config file", e);
            }
        }

        return modernPath;
    }

    private static class StatusBarImpl extends JToolBar implements StatusBar {
        private final JLabel infoLabel;

        public StatusBarImpl() {
            infoLabel = new JLabel((String) null);
            infoLabel.setVerticalAlignment(SwingConstants.CENTER);

            add(Box.createHorizontalStrut(10));
            add(infoLabel);
            add(Box.createHorizontalGlue());
            add(new MemoryIndicator());
        }

        @Nullable
        @Override
        public String getInfo() {
            return infoLabel.getText();
        }

        @Override
        public void setInfo(@Nullable String text) {
            infoLabel.setText(text);
        }
    }
}
