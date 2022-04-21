package com.shade.decima.ui.controls;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ExtendedTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, convertValueToText(value, selected), selected, expanded, leaf, row, hasFocus);

        Icon icon = getIcon(tree, value, expanded, leaf);

        if (icon != null && !tree.isEnabled()) {
            final Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(tree, icon);

            if (disabledIcon != null) {
                icon = disabledIcon;
            }
        }

        if (icon != null) {
            setIcon(icon);
        }

        return this;
    }

    @NotNull
    public String convertValueToText(@Nullable Object value, boolean selected) {
        if (value != null) {
            String text = value.toString();

            if (text != null && selected) {
                text = UIUtils.unescapeHtmlEntities(UIUtils.removeHtmlTags(text));
            }

            if (text != null) {
                return text;
            }
        }

        return "";
    }

    @Nullable
    public Icon getIcon(@NotNull JTree tree, @Nullable Object value, boolean expanded, boolean leaf) {
        return null;
    }
}
