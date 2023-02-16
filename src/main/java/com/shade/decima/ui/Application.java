package com.shade.decima.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.google.gson.Gson;
import com.shade.decima.BuildConfig;
import com.shade.decima.cli.ApplicationCLI;
import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.FileEditorInputLazy;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.decima.ui.navigator.NavigatorView;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.menu.ProjectCloseItem;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorChangeListener;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuService;
import com.shade.platform.ui.util.UIUtils;
import com.shade.platform.ui.views.ViewManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.prefs.Preferences;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Gson gson = new Gson();

    private static final Workspace workspace = new Workspace();
    private static final MenuService menuService = new MenuService();

    private static JFrame frame;
    private static ApplicationPane pane;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            UIUtils.showErrorDialog(getFrame(), exception);
            log.error("Unhandled exception", exception);
        });

        if (args.length > 0) {
            ApplicationCLI.execute(workspace, args);
        }

        SwingUtilities.invokeLater(() -> {
            configureUI(workspace.getPreferences());

            pane = new ApplicationPane();
            frame = new JFrame();

            installListeners();
            restoreState();

            frame.setContentPane(pane);
            frame.setTitle(getApplicationTitle());
            frame.setIconImages(FlatSVGUtils.createWindowIconImages("/icons/application.svg"));
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setVisible(true);

            getMenuService().installMenuBar(frame.getRootPane(), MenuConstants.APP_MENU_ID);
        });
    }

    @NotNull
    public static JFrame getFrame() {
        return frame;
    }

    @NotNull
    public static Workspace getWorkspace() {
        return workspace;
    }

    @NotNull
    public static MenuService getMenuService() {
        return menuService;
    }

    @NotNull
    public static EditorManager getEditorManager() {
        return pane.getEditorManager();
    }

    @NotNull
    public static ViewManager getViewManager() {
        return pane;
    }

    @NotNull
    public static NavigatorTree getNavigator() {
        return Objects.requireNonNull(Application.getViewManager().findView(NavigatorView.class)).getTree();
    }

    @NotNull
    private static String getApplicationTitle() {
        final Editor activeEditor = getEditorManager().getActiveEditor();
        if (activeEditor != null) {
            return BuildConfig.APP_TITLE + " - " + activeEditor.getInput().getName();
        } else {
            return BuildConfig.APP_TITLE;
        }
    }

    private static void installListeners() {
        final EditorManager manager = getEditorManager();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                final NavigatorTreeModel model = Application.getNavigator().getModel();

                for (ProjectContainer container : workspace.getProjects()) {
                    final NavigatorProjectNode node = model.getProjectNode(new VoidProgressMonitor(), container);

                    if (!node.needsInitialization() && !ProjectCloseItem.confirmProjectClose(node.getProject(), manager)) {
                        return;
                    }
                }

                saveState();
                System.exit(0);
            }
        });

        workspace.addProjectChangeListener(new ProjectChangeListener() {
            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                for (Editor editor : manager.getEditors()) {
                    if (editor.getInput() instanceof FileEditorInputLazy input && input.container().equals(container.getId())) {
                        manager.closeEditor(editor);
                    }
                }
            }

            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                for (Editor editor : manager.getEditors()) {
                    if (editor.getInput() instanceof FileEditorInput input && input.getProject().getContainer().equals(container)) {
                        manager.reuseEditor(editor, FileEditorInputLazy.from(input).canLoadImmediately(false));
                    } else if (editor.getInput() instanceof FileEditorInputLazy input && input.container().equals(container.getId())) {
                        manager.reuseEditor(editor, input.canLoadImmediately(false));
                    }
                }
            }
        });

        manager.addEditorChangeListener(new EditorChangeListener() {
            @Override
            public void editorChanged(@Nullable Editor editor) {
                frame.setTitle(getApplicationTitle());
            }
        });
    }

    private static void configureUI(@NotNull Preferences preferences) {
        FlatLaf.registerCustomDefaultsSource("themes");
        FlatInspector.install("ctrl shift alt X");
        FlatUIDefaultsInspector.install("ctrl shift alt Y");

        setLookAndFeel(preferences);

        UIManager.put("Action.containsIcon", new FlatSVGIcon("icons/actions/contains.svg"));
        UIManager.put("Action.editIcon", new FlatSVGIcon("icons/actions/edit.svg"));
        UIManager.put("Action.editModalIcon", new FlatSVGIcon("icons/actions/edit_modal.svg"));
        UIManager.put("Action.exportIcon", new FlatSVGIcon("icons/actions/export.svg"));
        UIManager.put("Action.importIcon", new FlatSVGIcon("icons/actions/import.svg"));
        UIManager.put("Action.packIcon", new FlatSVGIcon("icons/actions/pack.svg"));
        UIManager.put("Action.undoIcon", new FlatSVGIcon("icons/actions/undo.svg"));
        UIManager.put("Action.redoIcon", new FlatSVGIcon("icons/actions/redo.svg"));
        UIManager.put("Action.saveIcon", new FlatSVGIcon("icons/actions/save.svg"));
        UIManager.put("Action.searchIcon", new FlatSVGIcon("icons/actions/search.svg"));
        UIManager.put("Action.closeIcon", new FlatSVGIcon("icons/actions/tab_close.svg"));
        UIManager.put("Action.closeAllIcon", new FlatSVGIcon("icons/actions/tab_close_all.svg"));
        UIManager.put("Action.closeOthersIcon", new FlatSVGIcon("icons/actions/tab_close_others.svg"));
        UIManager.put("Action.closeUninitializedIcon", new FlatSVGIcon("icons/actions/tab_close_uninitialized.svg"));
        UIManager.put("Action.splitRightIcon", new FlatSVGIcon("icons/actions/split_right.svg"));
        UIManager.put("Action.splitDownIcon", new FlatSVGIcon("icons/actions/split_down.svg"));
        UIManager.put("Action.zoomInIcon", new FlatSVGIcon("icons/actions/zoom_in.svg"));
        UIManager.put("Action.zoomOutIcon", new FlatSVGIcon("icons/actions/zoom_out.svg"));
        UIManager.put("Action.zoomFitIcon", new FlatSVGIcon("icons/actions/zoom_fit.svg"));
        UIManager.put("Action.addElementIcon", new FlatSVGIcon("icons/actions/add_element.svg"));
        UIManager.put("Action.removeElementIcon", new FlatSVGIcon("icons/actions/remove_element.svg"));
        UIManager.put("Action.duplicateElementIcon", new FlatSVGIcon("icons/actions/duplicate_element.svg"));

        UIManager.put("Editor.binaryIcon", new FlatSVGIcon("icons/editors/binary.svg"));
        UIManager.put("Editor.coreIcon", new FlatSVGIcon("icons/editors/core.svg"));

        UIManager.put("Node.archiveIcon", new FlatSVGIcon("icons/nodes/archive.svg"));
        UIManager.put("Node.enumIcon", new FlatSVGIcon("icons/nodes/enum.svg"));
        UIManager.put("Node.uuidIcon", new FlatSVGIcon("icons/nodes/uuid.svg"));
        UIManager.put("Node.arrayIcon", new FlatSVGIcon("icons/nodes/array.svg"));
        UIManager.put("Node.objectIcon", new FlatSVGIcon("icons/nodes/object.svg"));
        UIManager.put("Node.referenceIcon", new FlatSVGIcon("icons/nodes/reference.svg"));
        UIManager.put("Node.decimalIcon", new FlatSVGIcon("icons/nodes/decimal.svg"));
        UIManager.put("Node.integerIcon", new FlatSVGIcon("icons/nodes/integer.svg"));
        UIManager.put("Node.stringIcon", new FlatSVGIcon("icons/nodes/string.svg"));
        UIManager.put("Node.booleanIcon", new FlatSVGIcon("icons/nodes/boolean.svg"));

        UIManager.put("Overlay.addIcon", new FlatSVGIcon("icons/overlays/add.svg"));
        UIManager.put("Overlay.modifyIcon", new FlatSVGIcon("icons/overlays/modify.svg"));
    }

    private static void setLookAndFeel(@NotNull Preferences pref) {
        final String lafClassName = pref.node("window").get("laf", FlatLightLaf.class.getName());

        try {
            UIManager.setLookAndFeel(lafClassName);
        } catch (Exception e) {
            log.error("Failed to setup look and feel '" + lafClassName + "'l: " + e);
        }
    }

    private static void saveState() {
        final Preferences pref = workspace.getPreferences();

        try {
            pref.node("editors").removeNode();
            pane.saveEditors(pref.node("editors"));
        } catch (Exception e) {
            log.warn("Unable to serialize editors", e);
        }

        try {
            pref.node("views").removeNode();
            pane.saveViews(pref.node("views"));
        } catch (Exception e) {
            log.warn("Unable to serialize views", e);
        }

        try {
            saveWindow(pref);
        } catch (Exception e) {
            log.warn("Unable to save window visuals", e);
        }
    }

    private static void restoreState() {
        final Preferences pref = workspace.getPreferences();

        try {
            pane.restoreEditors(pref.node("editors"));
        } catch (Exception e) {
            log.warn("Unable to restore editors", e);
        }

        try {
            pane.restoreViews(pref.node("views"));
        } catch (Exception e) {
            log.warn("Unable to restore views", e);
        }

        try {
            restoreWindow(pref.node("window"));
        } catch (Exception e) {
            log.warn("Unable to restore window visuals", e);
        }
    }

    private static void saveWindow(@NotNull Preferences pref) {
        final Preferences node = pref.node("window");
        node.putLong("size", (long) frame.getWidth() << 32 | frame.getHeight());
        node.putLong("location", (long) frame.getX() << 32 | frame.getY());
        node.putBoolean("maximized", (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) > 0);
    }

    private static void restoreWindow(@NotNull Preferences pref) {
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
}
