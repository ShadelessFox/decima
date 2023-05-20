package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueController.EditType;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Edit &Inline\u2026", icon = "Action.editIcon", keystroke = "ENTER", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 1000)
public class InlineEditItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final Tree tree = editor.getTree();
        tree.startEditingAtPath(tree.getSelectionPath());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final ValueController<Object> controller = editor.getValueController(EditType.INLINE);

        if (!editor.getTree().isEditing() && controller != null) {
            final ValueManager<Object> manager = ValueRegistry.getInstance().findManager(controller);
            return manager != null && manager.canEdit(EditType.INLINE);
        }

        return false;
    }
}
