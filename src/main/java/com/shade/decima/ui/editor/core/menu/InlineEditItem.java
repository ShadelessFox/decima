package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueManager;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Edit &Inline\u2026", icon = "Editor.editIcon", keystroke = "ENTER", group = CTX_MENU_CORE_EDITOR_GROUP_EDIT, order = 1000)
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
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);

        if (editor != null && selection instanceof CoreNodeObject node) {
            final ValueManager<Object> manager = ValueRegistry.getInstance().findManager(
                node.getValue(),
                node.getType(),
                editor.getInput().getProject().getContainer().getType()
            );

            return manager != null && manager.canEdit(ValueController.EditType.INLINE);
        }

        return false;
    }
}
