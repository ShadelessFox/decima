package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.command.ElementAddCommand;
import com.shade.decima.ui.editor.core.command.ElementAddCommand.Operation;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Add Element", icon = "Action.addElementIcon", keystroke = "alt INSERT", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT_ARRAY, order = 1000)
public class AddElementItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeObject node = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final RTTITypeArray<?> type = (RTTITypeArray<?>) node.getType();

        editor.getCommandManager().add(new ElementAddCommand(
            Operation.ADD,
            editor.getTree(),
            node,
            type.getComponentType().instantiate(),
            type.length(node.getValue())
        ));
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject obj
            && obj.getType() instanceof RTTITypeArray<?>;
    }
}
