package com.shade.decima.ui.editor.core;

import com.shade.decima.ui.data.ValueController;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class CoreTreeModel extends TreeModel {
    public CoreTreeModel(@NotNull Tree tree, @NotNull TreeNode root) {
        super(tree, root);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        final CoreTreeCellEditor editor = (CoreTreeCellEditor) tree.getCellEditor();
        final ValueController<Object> controller = editor.getController();

        controller.setValue(value);
    }
}
