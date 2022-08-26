package com.shade.decima.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.ui.menus.MenuService;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static final String APPLICATION_TITLE = "Decima Explorer";

    private static final MenuService menuService = new MenuService();

    private static ApplicationFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            FlatInspector.install("ctrl shift alt X");
            FlatUIDefaultsInspector.install("ctrl shift alt Y");

            UIManager.put("TitlePane.unifiedBackground", false);
            UIManager.put("TabbedPane.tabHeight", 24);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.hideMnemonics", false);
            UIManager.put("SplitPane.dividerSize", 7);
            UIManager.put("SplitPaneDivider.border", new SplitPaneDividerBorder());
            UIManager.put("FlatLaf.experimental.tree.widePathForLocation", true);
            UIManager.put(FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER, true);
            UIManager.put(FlatClientProperties.TABBED_PANE_SCROLL_BUTTONS_POLICY, FlatClientProperties.TABBED_PANE_POLICY_AS_NEEDED_SINGLE);

            UIManager.put("Navigator.binaryIcon", new FlatSVGIcon("icons/nodes/binary.svg"));
            UIManager.put("Editor.closeIcon", new FlatSVGIcon("icons/tab_close.svg"));
            UIManager.put("Editor.closeAllIcon", new FlatSVGIcon("icons/tab_close_all.svg"));
            UIManager.put("Editor.closeOthersIcon", new FlatSVGIcon("icons/tab_close_others.svg"));
            UIManager.put("Editor.splitRightIcon", new FlatSVGIcon("icons/split_right.svg"));
            UIManager.put("Editor.splitDownIcon", new FlatSVGIcon("icons/split_down.svg"));
            UIManager.put("Editor.zoomInIcon", new FlatSVGIcon("icons/zoom_in.svg"));
            UIManager.put("Editor.zoomOutIcon", new FlatSVGIcon("icons/zoom_out.svg"));
            UIManager.put("Editor.zoomFitIcon", new FlatSVGIcon("icons/zoom_fit.svg"));

            frame = new ApplicationFrame();
            frame.setJMenuBar(menuService.createMenuBar(MenuConstants.APP_MENU_ID));
            frame.setSize(1280, 720);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);

            menuService.createMenuKeyBindings(frame.getRootPane(), MenuConstants.APP_MENU_ID);

            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                UIUtils.showErrorDialog(exception);
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
        return menuService;
    }

    private static class SplitPaneDividerBorder extends FlatBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(UIManager.getColor("Component.borderColor"));
            g.fillRect(x, y, 1, height);
            g.fillRect(x + width - 1, y, 1, height);
        }
    }
}
