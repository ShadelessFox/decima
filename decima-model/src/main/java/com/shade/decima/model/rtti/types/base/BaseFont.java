package com.shade.decima.model.rtti.types.base;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.platform.model.Lazy;
import com.shade.util.NotNull;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

public abstract class BaseFont implements HwFont {
    protected final RTTIObject data;

    public BaseFont(@NotNull RTTIObject object) {
        if (!object.type().isInstanceOf("FontResource")) {
            throw new IllegalArgumentException("Not a FontResource: " + object.type());
        }

        this.data = object.obj("FontResourceData");
    }

    @Override
    public float getHeight() {
        return data.obj("TextMetrics").f32("Height");
    }

    @Override
    public float getAscent() {
        return data.obj("TextMetrics").f32("Ascent");
    }

    @Override
    public float getDescent() {
        return data.obj("TextMetrics").f32("Descent");
    }

    @Override
    public float getEmHeight() {
        return data.obj("TextMetrics").f32("EmHeight");
    }

    @NotNull
    @Override
    public String getName() {
        return data.str("TypefaceName");
    }

    protected abstract static class AbstractGlyph implements Glyph {
        protected final RTTIObject object;
        protected final Lazy<Path2D> path;

        public AbstractGlyph(@NotNull RTTIObject object) {
            this.object = object;
            this.path = Lazy.of(this::makePath);
        }

        @Override
        public float getAdvanceWidth() {
            return object.obj("GlyphMetrics").f32("AdvanceWidth");
        }

        @NotNull
        @Override
        public Rectangle2D getBounds() {
            final RTTIObject bounds = object.obj("GlyphContourList").obj("Bounds");
            return new Rectangle2D.Float(
                bounds.obj("Min").f32("X"),
                bounds.obj("Min").f32("Y"),
                bounds.obj("Max").f32("X") - bounds.obj("Min").f32("X"),
                bounds.obj("Max").f32("Y") - bounds.obj("Min").f32("Y")
            );
        }

        @NotNull
        @Override
        public Path2D getPath() {
            return path.get();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AbstractGlyph that = (AbstractGlyph) o;
            return object.equals(that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @NotNull
        private Path2D.Float makePath() {
            final RTTIObject[] contours = object.obj("GlyphContourList").objs("GlyphContours");
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

            return path;
        }
    }
}
