package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.dialogs.FindFileDialog;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import static com.shade.decima.ui.menu.MenuConstants.*;

public interface EditMenu {
    @MenuItemRegistration(parent = APP_MENU_EDIT_ID, name = "Find &Files\u2026", keystroke = "ctrl shift F", group = APP_MENU_EDIT_GROUP_GENERAL, order = 1000)
    class FindFilesItem extends MenuItem {
        @Override
        public void perform(@NotNull MenuItemContext ctx) {
            final Project project = findActiveProject();

            if (project == null) {
                return;
            }

            new FindFileDialog(Application.getFrame(), project).setVisible(true);
        }

        @Override
        public boolean isEnabled(@NotNull MenuItemContext ctx) {
            return findActiveProject() != null;
        }

        @Nullable
        private static Project findActiveProject() {
            final NavigatorTree navigator = Application.getFrame().getNavigator();

            if (navigator.getLastSelectedPathComponent() instanceof NavigatorNode node) {
                final NavigatorProjectNode root = node.getParentOfType(NavigatorProjectNode.class);

                if (!root.needsInitialization()) {
                    return root.getProject();
                }
            }

            return null;
        }
    }
}
