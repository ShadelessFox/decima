package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.dialog.FindReferencesDialog;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Find Local &References", keystroke = "alt F7", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 4000)
public class FindReferencesItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final var node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final var root = node.getParentOfType(CoreNodeBinary.class);
        final var object = (RTTIObject) node.getValue();

        new FindReferencesDialog(root, object).showDialog(Application.getFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject obj
            && obj.getType() instanceof RTTIClass cls
            && cls.isInstanceOf("RTTIRefObject");
    }
}
