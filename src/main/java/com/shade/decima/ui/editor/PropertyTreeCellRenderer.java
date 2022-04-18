package com.shade.decima.ui.editor;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.icon.overlay.FlatObjectCreatedOverlayIcon;
import com.shade.decima.ui.icon.overlay.FlatObjectModifiedOverlayIcon;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class PropertyTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String text = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);

        if (value != null && selected) {
            text = UIUtils.unescapeHtmlEntities(UIUtils.removeHtmlTags(text));
        }

        super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);

        if (value instanceof PropertyTreeNode node) {
            Icon icon = switch (node.getState()) {
                case MODIFIED -> getModifiedIcon(expanded, leaf);
                case CREATED -> getCreatedIcon(expanded, leaf);
                default -> null;
            };

            if (icon != null) {
                if (!tree.isEnabled()) {
                    final Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(tree, icon);
                    if (disabledIcon != null) {
                        icon = disabledIcon;
                    }
                }

                setIcon(icon);
            }
        }

        return this;
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
