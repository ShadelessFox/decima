package com.shade.decima.ui.menu.menus;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.dialogs.FindFileDialog;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

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

            if (navigator.getTree().getLastSelectedPathComponent() instanceof NavigatorNode node) {
                final NavigatorProjectNode root = UIUtils.getParentNode(node, NavigatorProjectNode.class);

                if (!root.needsInitialization()) {
                    return root.getProject();
                }
            }

            return null;
        }
    }
}
