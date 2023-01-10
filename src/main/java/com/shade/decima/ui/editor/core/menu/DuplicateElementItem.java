package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.RTTIType;
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

import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Duplicate Element", icon = "Action.duplicateElementIcon", keystroke = "ctrl D", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT_ARRAY, order = 2000)
public class DuplicateElementItem extends MenuItem {
    @SuppressWarnings("unchecked")
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeObject child = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final CoreNodeObject parent = (CoreNodeObject) Objects.requireNonNull(child.getParent());

        final RTTIType<Object> childType = (RTTIType<Object>) child.getType();
        final RTTITypeArray<Object> parentType = (RTTITypeArray<Object>) parent.getType();
        final int index = RemoveElementItem.indexOf(parentType, parent.getValue(), child.getValue());

        editor.getCommandManager().add(new ElementAddCommand(Operation.ADD, editor.getTree(), parent, childType.copyOf(child.getValue()), index + 1));
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject obj
            && obj.getParent() instanceof CoreNodeObject par
            && par.getType() instanceof RTTITypeArray<?>;
    }
}
