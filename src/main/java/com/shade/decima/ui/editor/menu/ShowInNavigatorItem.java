package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.lazy.LazyEditorInput;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;
import com.shade.decima.ui.navigator.NavigatorTree;

import javax.swing.tree.TreePath;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Show in Navigator", keystroke = "alt F1", group = CTX_MENU_EDITOR_STACK_GROUP_GENERAL, order = 1000)
public class ShowInNavigatorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);
        final NavigatorTree navigator = Application.getFrame().getNavigator();
        final TreePath path = new TreePath(navigator.getModel().getPathToRoot(editor.getInput().getNode()));

        navigator.setSelectionPath(path);
        navigator.scrollPathToVisible(path);
        navigator.requestFocusInWindow();
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);
        return editor != null && !(editor.getInput() instanceof LazyEditorInput);
    }
}
