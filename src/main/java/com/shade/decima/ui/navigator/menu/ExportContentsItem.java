package com.shade.decima.ui.navigator.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import static com.shade.decima.ui.menu.MenuConstants.*;
import static java.nio.file.StandardOpenOption.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Export Contents\u2026", icon = "Editor.exportIcon", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 2000)
public class ExportContentsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(node.getLabel()));

        if (chooser.showSaveDialog(Application.getFrame()) == JFileChooser.APPROVE_OPTION) {
            try (OutputStream os = Files.newOutputStream(chooser.getSelectedFile().toPath(), CREATE, WRITE, TRUNCATE_EXISTING)) {
                os.write(node.getPackfile().extract(node.getHash()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }
}
