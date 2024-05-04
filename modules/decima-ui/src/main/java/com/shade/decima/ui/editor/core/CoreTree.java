package com.shade.decima.ui.editor.core;

import com.shade.platform.ui.controls.tree.Tree;
import com.shade.util.NotNull;

import javax.swing.tree.TreePath;

public class CoreTree extends Tree {
    public CoreTree(@NotNull CoreNodeFile root) {
        setCellRenderer(new CoreTreeCellRenderer().withTags(this));
        setModel(new CoreTreeModel(this, root));
        setSelectionPath(new TreePath(root));
    }

    @Override
    public void togglePath(@NotNull TreePath path) {
        // do nothing
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
        if (value instanceof CoreNodeObject o) {
            final String text = o.getText();

            if (text != null) {
                return text;
            }
        }

        return super.convertValueToText(value, selected, expanded, leaf, row, focused);
    }

    @NotNull
    @Override
    public CoreTreeModel getModel() {
        return (CoreTreeModel) super.getModel();
    }
}
