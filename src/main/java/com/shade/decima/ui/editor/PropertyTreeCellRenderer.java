package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.ExtendedTreeCellRenderer;
import com.shade.decima.ui.icon.overlay.FlatObjectCreatedOverlayIcon;
import com.shade.decima.ui.icon.overlay.FlatObjectModifiedOverlayIcon;

import javax.swing.*;

public class PropertyTreeCellRenderer extends ExtendedTreeCellRenderer {
    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @Nullable Object value, boolean expanded, boolean leaf) {
        if (value instanceof PropertyTreeNode node) {
            return switch (node.getState()) {
                case CREATED -> getCreatedIcon(expanded, leaf);
                case MODIFIED -> getModifiedIcon(expanded, leaf);
                default -> null;
            };
        }

        return null;
    }

    @NotNull
    private Icon getModifiedIcon(boolean expanded, boolean leaf) {
        final Icon icon = getIcon();

        if (leaf) {
            return new FlatObjectModifiedOverlayIcon(icon, 9, 10);
        } else if (expanded) {
            return new FlatObjectModifiedOverlayIcon(icon, 11, 8);
        } else {
            return new FlatObjectModifiedOverlayIcon(icon, 11, 8);
        }
    }

    @NotNull
    private Icon getCreatedIcon(boolean expanded, boolean leaf) {
        final Icon icon = getIcon();

        if (leaf) {
            return new FlatObjectCreatedOverlayIcon(icon, 9, 10);
        } else if (expanded) {
            return new FlatObjectCreatedOverlayIcon(icon, 11, 8);
        } else {
            return new FlatObjectCreatedOverlayIcon(icon, 11, 8);
        }
    }
}
