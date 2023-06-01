package com.shade.decima.ui.bookmarks.impl;

import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.*;

public class BookmarkTreeCellRenderer extends ColoredTreeCellRenderer<TreeNode> {
    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof BookmarkNode node) {
            append(value.getLabel(), TextAttributes.REGULAR_ATTRIBUTES);
            append(" ", TextAttributes.REGULAR_ATTRIBUTES);
            append(node.getBookmark().location().path(), TextAttributes.GRAYED_ATTRIBUTES);
        }
    }
}
