package com.shade.decima.ui.menu.menus;

import com.shade.decima.ui.Application;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.net.URI;
import java.time.Year;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface HelpMenu {
    @MenuItemRegistration(parent = APP_MENU_HELP_ID, name = "&About", group = APP_MENU_HELP_GROUP_ABOUT, order = 1000)
    class AboutItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            JOptionPane.showMessageDialog(
                Application.getFrame(),
                new Object[]{
                    UIUtils.Labels.h1(Application.APPLICATION_TITLE),
                    "A tool for viewing and editing data in games powered by the Decima engine.",
                    "",
                    "Copyright \u00a9 2021-%s ShadelessFox".formatted(Year.now()),
                    UIUtils.Labels.link(URI.create("https://github.com/ShadelessFox/decima"))
                },
                "About",
                JOptionPane.INFORMATION_MESSAGE
            );

        }
    }
}
