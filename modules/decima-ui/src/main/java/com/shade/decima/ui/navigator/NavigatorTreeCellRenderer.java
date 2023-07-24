package com.shade.decima.ui.navigator;

import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorFolderNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class NavigatorTreeCellRenderer extends ColoredTreeCellRenderer<TreeNode> {
    private final TreeModel model;

    public NavigatorTreeCellRenderer(@NotNull TreeModel model) {
        this.model = model;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof NavigatorFolderNode node && node.getParent() instanceof NavigatorFolderNode parent) {
            final String[] parts = node.getPath().subpath(parent.getPath().length()).parts();

            for (int i = 0; i < parts.length; i++) {
                append(parts[i], TextAttributes.REGULAR_ATTRIBUTES);

                if (i < parts.length - 1) {
                    append(" / ", TextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        } else if (value instanceof NavigatorFileNode node && node.getSize() >= 0) {
            final boolean modified = node.getPackfile().hasChange(node.getPath());
            append("%s ".formatted(value.getLabel()), modified ? CommonTextAttributes.MODIFIED_ATTRIBUTES : TextAttributes.REGULAR_ATTRIBUTES);
            append(IOUtils.formatSize(node.getSize()), TextAttributes.GRAYED_SMALL_ATTRIBUTES);
        } else if (value instanceof NavigatorPackfileNode node && node.getPackfile().getInfo() != null && node.getPackfile().getInfo().lang() != null) {
            append("%s ".formatted(node.getPackfile().getName()), TextAttributes.REGULAR_ATTRIBUTES);
            append("(%s)".formatted(node.getPackfile().getInfo().lang().getLabel()), TextAttributes.GRAYED_ATTRIBUTES);
        } else if (value instanceof NavigatorProjectNode node && node.isOpen()) {
            append(value.getLabel(), TextAttributes.REGULAR_BOLD_ATTRIBUTES);
        } else if (value instanceof NavigatorProjectNode && model.isLoading(value)) {
            append(value.getLabel(), TextAttributes.REGULAR_ITALIC_ATTRIBUTES);
        } else {
            append(value.getLabel(), TextAttributes.REGULAR_ATTRIBUTES);
        }
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
