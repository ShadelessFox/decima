package com.shade.decima.ui.editor.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, icon = "Editor.closeAllIcon", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 3000)
public class CloseAllEditorsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final EditorStack stack = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY);

        for (Editor editor : manager.getEditors(stack)) {
            manager.closeEditor(editor);
        }
    }

    @Nullable
    @Override
    public String getName(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);

        if (manager.getStacksCount() > 1) {
            return "Close &Grouped Tabs";
        } else {
            return "Close &All Tabs";
        }
    }
}
