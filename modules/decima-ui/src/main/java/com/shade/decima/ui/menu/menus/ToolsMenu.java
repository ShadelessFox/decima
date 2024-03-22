package com.shade.decima.ui.menu.menus;

import com.shade.decima.ui.dialogs.HashToolDialog;
import com.shade.platform.ui.menus.*;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuRegistration(id = APP_MENU_TOOLS_ID, name = "&Tools", order = 3500)
public class ToolsMenu extends Menu {
    @MenuItemRegistration(parent = APP_MENU_TOOLS_ID, name = "&Hash Tool", description = "Open a tool for computing hashes of a string", group = APP_MENU_TOOLS_GROUP_GENERAL, order = 1000)
    public static class HashToolItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            HashToolDialog.open(JOptionPane.getRootFrame());
        }
    }
}
