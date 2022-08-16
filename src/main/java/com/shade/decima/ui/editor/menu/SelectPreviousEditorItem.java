package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Select Previous Tab", keystroke = "alt LEFT", group = CTX_MENU_EDITOR_STACK_GROUP_GENERAL, order = 0)
public class SelectPreviousEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        final Editor[] editors = manager.getEditors(ctx.getData(CommonDataKeys.EDITOR_STACK_KEY));
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);

        for (int i = 1; i < editors.length; i++) {
            if (editors[i] == editor) {
                manager.openEditor(editors[i - 1].getInput(), true);
                return;
            }
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        final Editor[] editors = manager.getEditors(ctx.getData(CommonDataKeys.EDITOR_STACK_KEY));
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);

        return editor != editors[0];
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return false;
    }
}
