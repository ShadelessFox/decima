package com.shade.platform.ui.editors.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.platform.ui.PlatformMenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "&Close", icon = "Action.closeIcon", keystroke = "ctrl F4", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 1000)
public class CloseCurrentEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);

        manager.closeEditor(editor);
    }
}
