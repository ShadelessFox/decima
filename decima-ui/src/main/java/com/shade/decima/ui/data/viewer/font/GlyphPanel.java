package com.shade.decima.ui.data.viewer.font;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class GlyphPanel extends JComponent {
    private static final int MARGIN = 20;

    private RTTIObject object;
    private GameType game;
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

    public void setObject(@NotNull RTTIObject object, @NotNull GameType game) {
        if (this.object != object || this.game != game) {
            this.object = object;
            this.game = game;

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

        final var data = object.obj("FontResourceData");
        final var codePoint = data.objs(game == GameType.HZD ? "CharInfo" : "CodePointInfo")[index];
        final var contourList = codePoint.obj("GlyphContourList");

        final var textMetrics = data.obj("TextMetrics");
        final var textDescent = textMetrics.f32("Descent");
        final var textEmHeight = textMetrics.f32("EmHeight");

        final var textSize = new Point2D.Float(codePoint.obj("GlyphMetrics").f32("AdvanceWidth"), textMetrics.f32("Height"));
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

            final RTTIObject bounds = contourList.obj("Bounds");
            final float glyphX = bounds.obj("Min").f32("X");
            final float glyphY = bounds.obj("Min").f32("Y");
            final float glyphWidth = bounds.obj("Max").f32("X") - glyphX;
            final float glyphHeight = bounds.obj("Max").f32("Y") - glyphY;

            g.setColor(Color.GREEN);
            g.drawRect((int) glyphX, (int) (textDescent + glyphY), (int) glyphWidth, (int) glyphHeight);
        }

        g.translate(0, textDescent);
        g.setColor(Color.WHITE);

        final RTTIObject[] contours = contourList.objs("GlyphContours");
        final Path2D.Float path = new Path2D.Float();

        for (final RTTIObject contour : contours) {
            final RTTIObject[] commands = contour.objs("CommandList");
            final RTTIObject[] points = contour.objs("Points");

            { // starting point
                final RTTIObject point = points[0];
                final float x = point.f32("X");
                final float y = point.f32("Y");

                path.moveTo(x, y);
            }

            int i = 0;

            for (RTTIObject command : commands) {
                final var cmd = command.i8("CmdData");
                final var curve = (cmd & 64) != 0;
                final var count = cmd & 63;

                if (curve) {
                    float x1 = points[i].f32("X");
                    float y1 = points[i].f32("Y");

                    for (int j = 1; j <= count - 1; j++) {
                        final float x2 = points[i + j].f32("X");
                        final float y2 = points[i + j].f32("Y");
                        float x3 = points[i + j + 1].f32("X");
                        float y3 = points[i + j + 1].f32("Y");

                        if (j < count - 1) {
                            x3 = (x2 + x3) / 2;
                            y3 = (y2 + y3) / 2;
                        }

                        path.curveTo(x1, y1, x2, y2, x3, y3);
                        x1 = x3;
                        y1 = y3;
                    }
                } else {
                    for (int j = 1; j <= count; j++) {
                        final RTTIObject point = points[i + j];
                        final float x = point.f32("X");
                        final float y = point.f32("Y");

                        path.lineTo(x, y);
                    }
                }

                i += count;
            }

            path.closePath();
        }

        g.fill(path);
    }
}
