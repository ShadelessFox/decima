package com.shade.platform.ui.editors.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.platform.ui.PlatformMenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Split and Move Down", icon = "Action.splitDownIcon", group = CTX_MENU_EDITOR_STACK_GROUP_SPLIT, order = 2000)
public class SplitAndMoveDownItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorStack stack = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY);
        final Editor editor = ctx.getData(PlatformDataKeys.EDITOR_KEY);

        stack.split(stack, editor, SwingConstants.SOUTH);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY).getTabCount() > 1;
    }
}
