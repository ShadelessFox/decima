package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "&Close", icon = "Editor.closeIcon", keystroke = "ctrl F4", group = CTX_MENU_EDITOR_STACK_GROUP_CLOSE, order = 1000)
public class CloseCurrentEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(CommonDataKeys.EDITOR_MANAGER_KEY);
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);

        manager.closeEditor(editor);
    }
}
