package com.shade.decima.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.shade.decima.cli.ApplicationCLI;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Lazy;
import com.shade.platform.ui.menus.MenuService;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.prefs.Preferences;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final Lazy<MenuService> menuService = Lazy.of(MenuService::new);

    private static ApplicationFrame frame;

    public static void main(String[] args) {
        final Workspace workspace = new Workspace();

        if (args.length > 0) {
            ApplicationCLI.execute(workspace, args);
        }

        SwingUtilities.invokeLater(() -> {
            FlatLaf.registerCustomDefaultsSource("themes");
            FlatInspector.install("ctrl shift alt X");
            FlatUIDefaultsInspector.install("ctrl shift alt Y");

            setLookAndFeel(workspace.getPreferences());

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

            final MenuService menuService = getMenuService();

            frame = new ApplicationFrame(workspace);
            frame.setJMenuBar(menuService.createMenuBar(MenuConstants.APP_MENU_ID));
            frame.setIconImages(FlatSVGUtils.createWindowIconImages("/icons/application.svg"));

            menuService.createMenuKeyBindings(frame.getRootPane(), MenuConstants.APP_MENU_ID);

            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                UIUtils.showErrorDialog(getFrame(), exception);
                log.error("Unhandled exception", exception);
            });
        });
    }

    @Deprecated
    @NotNull
    public static ApplicationFrame getFrame() {
        return frame;
    }

    @NotNull
    public static MenuService getMenuService() {
        return menuService.get();
    }

    private static void setLookAndFeel(@NotNull Preferences prefs) {
        final String lafClassName = prefs.node("window").get("laf", FlatLightLaf.class.getName());

        try {
            UIManager.setLookAndFeel(lafClassName);
        } catch (Exception e) {
            log.error("Failed to setup look and feel '" + lafClassName + "'l: " + e);
        }
    }
}
