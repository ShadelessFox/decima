package com.shade.decima.model.rtti.types.java;

import com.shade.util.NotNull;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public interface HwFont {
    float getHeight();

    float getAscent();

    float getDescent();

    float getEmHeight();

    int getGlyphCount();

    @NotNull
    Glyph getGlyph(int index);

    @NotNull
    String getName();

    interface Glyph {
        int getCodePoint();

        float getAdvanceWidth();

        @NotNull
        Rectangle2D getBounds();

        @NotNull
        Path2D getPath();
    }
}
