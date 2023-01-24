package com.shade.decima.ui.editor.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Close &Other Tabs", icon = "Action.closeOthersIcon", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 2000)
public class CloseOtherEditorsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final EditorStack stack = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY);

        for (Editor e : manager.getEditors(stack)) {
            if (e != editor) {
                manager.closeEditor(e);
            }
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final EditorStack stack = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY);
        return manager != null && manager.getEditorsCount(stack) > 1;
    }
}
