package com.shade.platform.ui.editors.menu;

import com.shade.platform.model.util.MathUtils;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.platform.ui.PlatformMenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Select Next Tab", keystroke = "alt RIGHT", group = CTX_MENU_EDITOR_STACK_GROUP_GENERAL, order = 0)
public class SelectNextEditorItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorManager manager = ctx.getData(PlatformDataKeys.EDITOR_MANAGER_KEY);
        final Editor[] editors = manager.getEditors(ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY));
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);

        for (int i = 0; i < editors.length; i++) {
            if (editors[i] == editor) {
                final int index = MathUtils.wrapAround(i + 1, editors.length);
                manager.openEditor(editors[index].getInput(), true);
                return;
            }
        }
    }

    @Override
    public boolean isEnabled(@NotNull MenuItemContext ctx) {
        return true;
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return false;
    }
}
