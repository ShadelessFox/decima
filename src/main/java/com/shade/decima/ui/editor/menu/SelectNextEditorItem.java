package com.shade.decima.ui.editor.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Select Next Tab", keystroke = "alt RIGHT", group = CTX_MENU_EDITOR_STACK_GROUP_GENERAL, order = 0)
public class SelectNextEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final Editor[] editors = manager.getEditors(ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY));
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);

        for (int i = 0; i < editors.length - 1; i++) {
            if (editors[i] == editor) {
                manager.openEditor(editors[i + 1].getInput(), true);
                return;
            }
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final Editor[] editors = manager.getEditors(ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY));
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);

        return editor != editors[editors.length - 1];
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return false;
    }
}
