package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.dialogs.FindFilesDialog;
import com.shade.decima.ui.dialogs.FindFilesDialog.Strategy;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, id = CTX_MENU_NAVIGATOR_FIND_ID, name = "Find files that\u2026", group = CTX_MENU_NAVIGATOR_GROUP_FIND, order = 1000)
public class FindFilesItem extends MenuItem {
    @MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_FIND_ID, name = "\u2026 are referenced by this file", group = CTX_MENU_NAVIGATOR_FIND_GROUP_GENERAL, order = 2000)
    public static class FindReferencedByItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
            final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            FindFilesDialog.show(JOptionPane.getRootFrame(), project, Strategy.FIND_REFERENCED_BY, node.getPath().full());
        }
    }

    @MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_FIND_ID, name = "\u2026 reference this file", group = CTX_MENU_NAVIGATOR_FIND_GROUP_GENERAL, order = 1000)
    public static class FindReferencesToItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
            final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
            FindFilesDialog.show(JOptionPane.getRootFrame(), project, Strategy.FIND_REFERENCES_TO, node.getPath().full());
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }
}
