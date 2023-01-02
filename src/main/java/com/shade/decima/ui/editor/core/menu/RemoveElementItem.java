package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.decima.ui.editor.core.command.ElementRemoveCommand;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import java.util.Objects;

import static com.shade.decima.ui.menu.MenuConstants.CTX_MENU_CORE_EDITOR_ID;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Remove Element", icon = "CoreEditor.removeElementIcon", keystroke = "DELETE", group = MenuConstants.CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 4000)
public class RemoveElementItem extends MenuItem {
    @SuppressWarnings("unchecked")
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeObject child = (CoreNodeObject) ctx.getData(PlatformDataKeys.SELECTION_KEY);
        final CoreNodeObject parent = (CoreNodeObject) Objects.requireNonNull(child.getParent());

        final RTTITypeArray<Object> parentType = (RTTITypeArray<Object>) parent.getType();
        final int index = indexOf(parentType, parent.getValue(), child.getValue());

        editor.getCommandManager().add(new ElementRemoveCommand(editor.getTree(), parent, child.getValue(), index));
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeObject obj
            && obj.getParent() instanceof CoreNodeObject par
            && par.getType() instanceof RTTITypeArray<?>;
    }

    private static int indexOf(@NotNull RTTITypeArray<Object> type, @NotNull Object array, @NotNull Object element) {
        for (int i = 0, length = type.length(array); i < length; i++) {
            if (element.equals(type.get(array, i))) {
                return i;
            }
        }

        throw new IllegalArgumentException("The supplied element is not present in the array");
    }
}
