package com.shade.platform.ui.editors.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.platform.ui.PlatformMenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "&Move to Opposite Group", group = CTX_MENU_EDITOR_STACK_GROUP_SPLIT, order = 3000)
public class MoveToOppositeGroupItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);
        EditorStack source = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY);
        EditorStack target = source.getOpposite();

        if (target != null) {
            source.move(editor, target);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY).getOpposite() != null;
    }
}
