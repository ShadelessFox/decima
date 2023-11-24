package com.shade.platform.ui.controls.tree;

import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class TreeCellRenderer extends ColoredTreeCellRenderer<TreeNode> {
    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        append(value.getLabel(), TextAttributes.REGULAR_ATTRIBUTES);
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (!value.hasIcon()) {
            return null;
        }
        final Icon icon = value.getIcon();
        if (icon != null) {
            return icon;
        } else {
            return super.getIcon(tree, value, selected, expanded, focused, leaf, row);
        }
    }
}
