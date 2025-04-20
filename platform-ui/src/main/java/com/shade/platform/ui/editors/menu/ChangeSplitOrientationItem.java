package com.shade.platform.ui.editors.menu;

import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.editors.stack.EditorStackContainer;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

import static com.shade.platform.ui.PlatformMenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_EDITOR_STACK_ID, name = "Change Split O&rientation", group = CTX_MENU_EDITOR_STACK_GROUP_SPLIT, order = 4000)
public class ChangeSplitOrientationItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        EditorStackContainer container = ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY).getContainer().getSplitContainer();
        if (container == null) {
            return;
        }

        if (container.getSplitOrientation() == JSplitPane.VERTICAL_SPLIT) {
            container.setSplitOrientation(JSplitPane.HORIZONTAL_SPLIT);
        } else {
            container.setSplitOrientation(JSplitPane.VERTICAL_SPLIT);
        }
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.EDITOR_STACK_KEY).getContainer().getSplitContainer() != null;
    }
}
