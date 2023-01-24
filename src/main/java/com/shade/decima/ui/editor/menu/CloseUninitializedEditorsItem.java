package com.shade.decima.ui.editor.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Close &Uninitialized Tabs", icon = "Action.closeUninitializedIcon", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 4000)
public class CloseUninitializedEditorsItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);

        for (Editor editor : manager.getEditors()) {
            if (editor.getInput() instanceof LazyEditorInput input && !input.canLoadImmediately()) {
                manager.closeEditor(editor);
            }
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);

        for (Editor editor : manager.getEditors()) {
            if (editor.getInput() instanceof LazyEditorInput input && !input.canLoadImmediately()) {
                return true;
            }
        }

        return false;
    }
}
