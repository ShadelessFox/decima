package com.shade.platform.ui.controls.plaf;

import com.formdev.flatlaf.ui.FlatSplitPaneUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

@SuppressWarnings("unused")
public class ThinFlatSplitPaneUI extends FlatSplitPaneUI {
    @SuppressWarnings("unused")
    public static ComponentUI createUI(JComponent c) {
        return new ThinFlatSplitPaneUI();
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new ThinFlatSplitPaneDivider(this);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        splitPane.setDividerSize(1);
        divider.setDividerSize(1);
        dividerSize = 1;
    }

    private class ThinFlatSplitPaneDivider extends FlatSplitPaneDivider {
        private static final int DRAG_SIZE = 4;
        private static final int DRAG_OFFSET = DRAG_SIZE * 2;

        protected ThinFlatSplitPaneDivider(BasicSplitPaneUI ui) {
            super(ui);
            setBackground(UIManager.getColor("Separator.foreground"));
            setLayout(new ThinFlatDividerLayout());
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(getBackground());

            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                g.drawLine(DRAG_SIZE, 0, DRAG_SIZE, getHeight() - 1);
            } else {
                g.drawLine(0, DRAG_SIZE, getWidth() - 1, DRAG_SIZE);
            }
        }

        @Override
        protected void dragDividerTo(int location) {
            super.dragDividerTo(location + DRAG_SIZE);
        }

        @Override
        protected void finishDraggingTo(int location) {
            super.finishDraggingTo(location + DRAG_SIZE);
        }

        private class ThinFlatDividerLayout extends FlatDividerLayout {
            @Override
            public void layoutContainer(Container c) {
                super.layoutContainer(c);

                final Rectangle bounds = c.getBounds();

                if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                    bounds.x -= DRAG_SIZE;
                    bounds.width += DRAG_OFFSET;
                } else {
                    bounds.y -= DRAG_SIZE;
                    bounds.height += DRAG_OFFSET;
                }

                setBounds(bounds);
            }
        }
    }
}
