package com.shade.decima.ui.navigator.menu;

import com.formdev.flatlaf.util.SystemInfo;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Show in Explorer", group = CTX_MENU_NAVIGATOR_GROUP_GENERAL, order = 1000)
public class ShowInExplorerItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);

        if (selection instanceof NavigatorPackfileNode node) {
            browseFileDirectory(node.getPackfile().getPath());
        } else if (selection instanceof NavigatorProjectNode node) {
            browseFileDirectory(node.getProjectContainer().getExecutablePath());
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);

        return selection instanceof NavigatorProjectNode
            || selection instanceof NavigatorPackfileNode;
    }

    private static void browseFileDirectory(@NotNull Path path) {
        final Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR) && SystemInfo.isWindows) {
            try {
                Runtime.getRuntime().exec("explorer /E,/select=" + path);
                return;
            } catch (IOException ignored) {
            }
        }

        desktop.browseFileDirectory(path.toFile());
    }
}
