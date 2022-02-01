package com.shade.decima.ui.editors;

import com.shade.decima.ui.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class PropertyTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value != null && selected) {
            value = UIUtils.unescapeHtmlEntities(UIUtils.removeHtmlTags(tree.convertValueToText(value, true, expanded, leaf, row, hasFocus)));
        }

        return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
}
