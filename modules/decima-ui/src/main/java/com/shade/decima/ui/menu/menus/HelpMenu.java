package com.shade.decima.ui.menu.menus;

import com.shade.decima.BuildConfig;
import com.shade.decima.ui.updater.UpdateService;
import com.shade.platform.ui.menus.Menu;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.*;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Properties;

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
                UIUtils.showErrorDialog(e, "Unable to open wiki page");
            }
        }
    }

    @MenuItemRegistration(parent = APP_MENU_HELP_ID, name = "&Check for Updates\u2026", group = APP_MENU_HELP_GROUP_ABOUT, order = 1000)
    public static class CheckForUpdatesItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            UpdateService.getInstance().checkForUpdatesModal(JOptionPane.getRootFrame());
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
            Properties p = System.getProperties();
            String text = MESSAGE.format(new Object[]{
                BuildConfig.APP_TITLE,
                BuildConfig.APP_VERSION, BuildConfig.BUILD_TIME, BuildConfig.BUILD_COMMIT,
                p.get("java.version"), p.get("java.vm.name"), p.get("java.vm.version"), p.get("java.vm.info"),
                p.get("java.vendor"), p.get("java.vendor.url")
            });

            JOptionPane.showMessageDialog(
                JOptionPane.getRootFrame(),
                UIUtils.createBrowseText(text),
                "About",
                JOptionPane.PLAIN_MESSAGE
            );

        }
    }
}
