package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.FileChooser;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.shade.decima.ui.menu.MenuConstants.*;
import static java.nio.file.StandardOpenOption.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "E&xport File\u2026", icon = "Action.exportIcon", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 1000)
public class ExportContentsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorFileNode node = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final FilePath path = node.getPath();

        final JCheckBox keepDirectoryStructure = new JCheckBox("Keep directory structure");
        keepDirectoryStructure.setToolTipText("Creates subdirectories for the exported file");

        final FileChooser chooser = new FileChooser();
        chooser.setSelectedFile(new File(path.last()));
        chooser.setOptions(keepDirectoryStructure);

        if (chooser.showSaveDialog(JOptionPane.getRootFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            final Path file = chooser.getSelectedFile().toPath();
            final Path output;

            if (keepDirectoryStructure.isSelected() && path.length() > 1) {
                output = file.resolveSibling(path.full());
                Files.createDirectories(output.getParent());
            } else {
                output = file;
            }

            try (
                InputStream is = node.getFile().newInputStream();
                OutputStream os = Files.newOutputStream(output, CREATE, WRITE, TRUNCATE_EXISTING)
            ) {
                is.transferTo(os);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }
}
