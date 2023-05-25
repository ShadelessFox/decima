package com.shade.decima.ui.editor.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.NavigatorView;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.PlatformMenuConstants;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.platform.ui.views.ViewManager;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

@MenuItemRegistration(parent = PlatformMenuConstants.CTX_MENU_EDITOR_STACK_ID, name = "Show in Navigator", keystroke = "alt F1", group = PlatformMenuConstants.CTX_MENU_EDITOR_STACK_GROUP_GENERAL, order = 1000)
public class ShowInNavigatorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final NavigatorTree navigator = Application.getNavigator();
        final NodeEditorInput input = (NodeEditorInput) ctx.getData(PlatformDataKeys.EDITOR_KEY).getInput();
        final TreePath path = navigator.getModel().getTreePathToRoot(input.getNode());

        navigator.setSelectionPath(path);
        navigator.scrollPathToVisible(path);
        navigator.requestFocusInWindow();

        ViewManager.getInstance().showView(NavigatorView.ID);
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);
        return editor != null && editor.getInput() instanceof NodeEditorInput;
    }
}
