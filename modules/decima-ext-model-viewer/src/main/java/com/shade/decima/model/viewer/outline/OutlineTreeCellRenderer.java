package com.shade.decima.model.viewer.outline;

import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeCellRenderer;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class OutlineTreeCellRenderer extends TreeCellRenderer {
    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof OutlineTreeNode node && !isVisible(node)) {
            append(node.getLabel(), node.getNode().isVisible() ? TextAttributes.GRAYED_ITALIC_ATTRIBUTES : TextAttributes.GRAYED_ATTRIBUTES);
        } else {
            super.customizeCellRenderer(tree, value, selected, expanded, focused, leaf, row);
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        return null;
    }

    private static boolean isVisible(@NotNull TreeNode node) {
        return node instanceof OutlineTreeNode n
            && n.getNode().isVisible()
            && (n.getParent() == null || isVisible(n.getParent()));
    }
}
