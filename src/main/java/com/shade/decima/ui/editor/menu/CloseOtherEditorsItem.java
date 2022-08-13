package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Close &Other Tabs", icon = "Editor.closeOthersIcon", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 2000)
public class CloseOtherEditorsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);

        if (manager == null || editor == null) {
            return;
        }

        for (Editor e : manager.getEditors()) {
            if (e != editor) {
                manager.closeEditor(e);
            }
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        return manager != null && manager.getEditorsCount() > 1;
    }
}
