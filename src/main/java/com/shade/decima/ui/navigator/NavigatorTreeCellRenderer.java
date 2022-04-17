package com.shade.decima.ui.navigator;

import com.shade.decima.ui.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class NavigatorTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String text = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);

        if (value != null && selected) {
            text = UIUtils.unescapeHtmlEntities(UIUtils.removeHtmlTags(text));
        }

        return super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
    }

}
