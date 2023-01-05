package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "&Group By Type", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 1000)
public class GroupEntriesByTypeItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeBinary binary = (CoreNodeBinary) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        if (editor.isDirty()) {
            // FIXME: Toggling grouping breaks command as they point to the old nodes,
            //        so prompt to save everything before continuing

            final int result = JOptionPane.showConfirmDialog(
                Application.getFrame(),
                "Do you want to save changes to '%s'?".formatted(editor.getInput().getName()),
                "Confirm Save",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                editor.doSave(new VoidProgressMonitor());
            } else if (result == JOptionPane.NO_OPTION) {
                editor.doReset();
            } else {
                return;
            }
        }

        binary.setGroupingEnabled(!binary.isGroupingEnabled());
        binary.unloadChildren();
        editor.getTree().getModel().fireStructureChanged(binary);
        editor.getTree().setSelectionPath(new TreePath(binary));
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeBinary binary
            && binary.isGroupingEnabled();
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeBinary;
    }
}
