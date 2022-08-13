package com.shade.decima.ui.editor.menu;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.CommonDataKeys;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.stack.EditorStack;
import com.shade.decima.ui.menu.MenuItem;
import com.shade.decima.ui.menu.MenuItemContext;
import com.shade.decima.ui.menu.MenuItemRegistration;

import javax.swing.*;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Split and Move Right", icon = "Editor.splitRightIcon", group = CTX_MENU_EDITOR_STACK_GROUP_SPLIT, order = 1000)
public class SplitAndMoveRightItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final EditorStack stack = ctx.getData(CommonDataKeys.EDITOR_STACK_KEY);
        final Editor editor = ctx.getData(CommonDataKeys.EDITOR_KEY);

        stack.split(stack, editor, SwingConstants.EAST);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(CommonDataKeys.EDITOR_STACK_KEY).getTabCount() > 1;
    }
}
