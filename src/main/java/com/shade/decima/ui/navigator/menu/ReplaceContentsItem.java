package com.shade.decima.ui.navigator.menu;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectPersister;
import com.shade.decima.model.packfile.resource.FileResource;
import com.shade.decima.model.packfile.resource.Resource;
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
import java.nio.file.Path;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_NAVIGATOR_ID, name = "Replace Contents\u2026", icon = "Editor.importIcon", group = CTX_MENU_NAVIGATOR_GROUP_EDIT, order = 1000)
public class ReplaceContentsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final Project project = ctx.getData(CommonDataKeys.PROJECT_KEY);
        final NavigatorFileNode file = (NavigatorFileNode) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        project.getPersister().addChange(file, new FileChange(chooser.getSelectedFile().toPath(), file.getHash()));
        Application.getFrame().getNavigator().getModel().fireNodesChanged(file);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof NavigatorFileNode;
    }

    private record FileChange(@NotNull Path path, long hash) implements ProjectPersister.Change {
        @NotNull
        @Override
        public ProjectPersister.Change merge(@NotNull ProjectPersister.Change change) {
            if (change instanceof FileChange) {
                return change;
            } else {
                throw new IllegalArgumentException("Can't merge with " + change);
            }
        }

        @NotNull
        @Override
        public Resource toResource() throws IOException {
            return new FileResource(path, hash);
        }
    }
}
