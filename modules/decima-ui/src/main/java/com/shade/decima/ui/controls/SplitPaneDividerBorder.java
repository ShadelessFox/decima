package com.shade.decima.ui.controls;

import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.platform.ui.UIColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.*;

public class SplitPaneDividerBorder extends FlatBorder {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final JSplitPane pane = ((BasicSplitPaneDivider) c).getBasicSplitPaneUI().getSplitPane();
        final boolean vertical = pane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;

        g.setColor(UIColor.SHADOW);

        if (pane.getLeftComponent() != null) {
            if (vertical) {
                g.fillRect(x, y, 1, height);
            } else {
                g.fillRect(x, y, width, 1);
            }
        }

        if (pane.getRightComponent() != null) {
            if (vertical) {
                g.fillRect(x + width - 1, y, 1, height);
            } else {
                g.fillRect(x, y + height - 1, width, 1);
            }
        }
    }
}
