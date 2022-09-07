package com.shade.decima.ui.controls;

import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Adapted from <a href="https://tips4java.wordpress.com/2008/11/06/wrap-layout/">https://tips4java.wordpress.com/2008/11/06/wrap-layout/</a>
 * with slights modifications to allow seamless resize within {@link JSplitPane}.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(@NotNull Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;

            if (targetWidth == 0)
                targetWidth = Integer.MAX_VALUE;

            final Insets insets = target.getInsets();
            final int horizontalInsetsAndGap = insets.left + insets.right + (getHgap() * 2);
            final int maxWidth = targetWidth - horizontalInsetsAndGap;
            final Dimension dim = new Dimension(0, 0);

            int rowWidth = 0;
            int rowHeight = 0;

            for (int i = 0; i < target.getComponentCount(); i++) {
                final Component m = target.getComponent(i);

                if (m.isVisible()) {
                    final Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }

                    if (preferred) {
                        rowWidth += d.width;
                    } else {
                        rowWidth = Math.max(rowWidth, d.width);
                    }

                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + getVgap() * 2;

            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null) {
                dim.width -= (getHgap() + 1);
            }

            return dim;
        }
    }

    private void addRow(@NotNull Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }
}
