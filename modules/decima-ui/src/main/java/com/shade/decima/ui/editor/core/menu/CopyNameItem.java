package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Copy Attribute Name", keystroke = "ctrl shift C", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 4000)
public class CopyNameItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        final StringSelection contents = new StringSelection(node.getLabel());
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        clipboard.setContents(contents, contents);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject node
            && node.getParent() instanceof CoreNodeObject parent && parent.getType() instanceof RTTIClass;
    }
}
