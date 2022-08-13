package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.stack.EditorStack;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, icon = "Editor.closeAllIcon", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 3000)
public class CloseAllEditorsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        final EditorStack stack = ctx.getData(CommonDataKeys.EDITOR_STACK_KEY);

        for (Editor editor : manager.getEditors(stack)) {
            manager.closeEditor(editor);
        }
    }

    @Nullable
    @Override
    public String getName(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);

        if (manager.getStacksCount() > 1) {
            return "Close &Grouped Tabs";
        } else {
            return "Close &All Tabs";
        }
    }
}
