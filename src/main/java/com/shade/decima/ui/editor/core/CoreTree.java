package com.shade.decima.ui.editor.core;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class CoreTree extends Tree {
    public CoreTree(@NotNull CoreNodeBinary root) {
        super(root, CoreTreeModel::new);
        setCellRenderer(new CoreTreeCellRenderer(getModel()));
        setSelectionPath(new TreePath(root));
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
        if (value instanceof CoreNodeObject o) {
            final String text = o.getHandler().getString(o.getType(), o.getObject());

            if (text != null) {
                return text;
            }
        }

        return super.convertValueToText(value, selected, expanded, leaf, row, focused);
    }
}
