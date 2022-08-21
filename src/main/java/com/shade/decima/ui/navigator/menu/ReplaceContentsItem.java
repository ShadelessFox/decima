package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.resource.FileResource;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.IOException;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Replace Contents\u2026", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 1000)
public class ReplaceContentsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
        final NavigatorFileNode file = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final FileResource resource;

        try {
            resource = new FileResource(chooser.getSelectedFile().toPath(), file.getHash());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        project.getPersister().addChange(file, resource);
        Application.getFrame().getNavigator().getModel().fireNodeChanged(file);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }
}
