package com.shade.decima.ui.navigator;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.ExtendedTreeCellRenderer;
import com.shade.decima.ui.icon.Icons;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;

public class NavigatorTreeCellRenderer extends ExtendedTreeCellRenderer {
    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @Nullable Object value, boolean expanded, boolean leaf) {
        if (leaf && value instanceof NavigatorFileNode node && node.getLabel().indexOf('.') < 0) {
            return Icons.NODE_BINARY;
        }

        return null;
    }
}
