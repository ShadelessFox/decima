package com.shade.decima.ui.controls.plaf;

import com.formdev.flatlaf.ui.FlatSeparatorUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.ui.controls.LabeledSeparator;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;

import static com.formdev.flatlaf.util.UIScale.scale;

public class LabeledSeparatorUI extends FlatSeparatorUI {
    protected Color labelForeground;

    public LabeledSeparatorUI(boolean shared) {
        super(shared);
    }

    public static ComponentUI createUI(JComponent c) {
        return FlatUIUtils.canUseSharedUI(c)
            ? FlatUIUtils.createSharedUI(LabeledSeparatorUI.class, () -> new LabeledSeparatorUI(true))
            : new LabeledSeparatorUI(false);
    }

    @Override
    protected void installDefaults(JSeparator s) {
        super.installDefaults(s);
        labelForeground = UIManager.getColor("LabeledSeparator.labelForeground");
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            UIUtils.setRenderingHints(g2);

            final String label = ((LabeledSeparator) c).getLabel();
            final float width = scale((float) stripeWidth);
            final float indent = scale((float) stripeIndent);
            final float height = getHeight(c);
            final float shift;

            if (label != null && !label.isEmpty()) {
                final FontMetrics metrics = c.getFontMetrics(g.getFont());

                g2.setColor(labelForeground);
                g2.drawString(label, indent, metrics.getAscent());

                shift = scale(metrics.stringWidth(label) + 5f);
            } else {
                shift = 0;
            }

            g2.setColor(c.getForeground());
            g2.fill(new Rectangle2D.Float(shift, indent + height / 2, c.getWidth() - shift, width));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(0, getHeight(c));
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    private int getHeight(@NotNull JComponent c) {
        final String label = ((LabeledSeparator) c).getLabel();

        if (label != null && !label.isEmpty()) {
            return c.getFontMetrics(c.getFont()).getHeight();
        } else {
            return scale(this.height);
        }
    }
}
