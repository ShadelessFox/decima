package com.shade.decima.ui.editor.core.menu.root;

import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BaseToggleItem extends MenuItem implements MenuItem.Check {
    private final Function<CoreNodeBinary, Boolean> getter;
    private final BiConsumer<CoreNodeBinary, Boolean> setter;

    public BaseToggleItem(@NotNull Function<CoreNodeBinary, Boolean> getter, @NotNull BiConsumer<CoreNodeBinary, Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreNodeBinary binary = (CoreNodeBinary) ctx.getData(PlatformDataKeys.SELECTION_KEY);

        if (editor.isDirty()) {
            // FIXME: Toggling grouping breaks command as they point to the old nodes,
            //        so prompt to save everything before continuing

            final int result = JOptionPane.showConfirmDialog(
                Application.getInstance().getFrame(),
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

        final TreePath path = new TreePath(binary);

        setter.accept(binary, !getter.apply(binary));
        binary.unloadChildren();

        editor.getTree().getModel().fireStructureChanged(binary);
        editor.getTree().setSelectionPath(path);
        editor.getBreadcrumbBar().setPath(path, false);
    }

    @Override
    public boolean isChecked(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeBinary binary
            && getter.apply(binary);
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        return ctx.getData(PlatformDataKeys.SELECTION_KEY) instanceof CoreNodeBinary;
    }
}
