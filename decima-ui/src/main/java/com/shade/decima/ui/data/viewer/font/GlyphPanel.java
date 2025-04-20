package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class GlyphPanel extends JComponent {
    private static final int MARGIN = 20;

    private HwFont input;
    private int index = -1;
    private boolean showDetails;

    @Override
    protected void paintComponent(Graphics g) {
        if (index < 0) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.0f));

            doPaint(g2);
        } finally {
            g2.dispose();
        }
    }

    public void setInput(@NotNull HwFont input) {
        if (this.input != input) {
            this.input = input;

            repaint();
        }
    }

    public void setIndex(int index) {
        if (this.index != index) {
            this.index = index;

            repaint();
        }
    }

    public void setShowDetails(boolean showDetails) {
        if (this.showDetails != showDetails) {
            this.showDetails = showDetails;

            repaint();
        }
    }

    private void doPaint(@NotNull Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        final var textHeight = input.getHeight();
        final var textDescent = input.getDescent();
        final var textEmHeight = input.getEmHeight();
        final var glyph = input.getGlyph(index);

        final var textSize = new Point2D.Float(glyph.getAdvanceWidth(), textHeight);
        final var viewSize = new Point2D.Float(getWidth() - MARGIN * 2, getHeight() - MARGIN * 2);

        { // scale graphics to the bounds of the glyph
            final var scale = UIUtils.getScalingFactor(viewSize.x, viewSize.y, textSize.x, textSize.y);
            g.translate((viewSize.x - textSize.x * scale) / 2 + MARGIN, (viewSize.y - textSize.y * scale) / 2 + MARGIN);
            g.scale(scale, -scale);
            g.translate(0, -textSize.y);
        }

        if (showDetails) {
            g.setColor(Color.GRAY);
            g.drawRect(0, 0, (int) textSize.x, (int) textSize.y);
            g.drawLine(0, (int) textDescent, (int) textSize.x, (int) textDescent);
            g.drawLine(0, (int) (textDescent + textEmHeight), (int) textSize.x, (int) (textDescent + textEmHeight));

            final Rectangle2D bounds = glyph.getBounds();

            g.setColor(Color.GREEN);
            g.drawRect((int) bounds.getX(), (int) (textDescent + bounds.getY()), (int) bounds.getWidth(), (int) bounds.getHeight());
        }

        g.translate(0, textDescent);
        g.setColor(Color.WHITE);
        g.fill(glyph.getPath());
    }
}
