package com.shade.decima.model.rtti.types.ds;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.base.BaseFont;
import com.shade.util.NotNull;

public class DSFont extends BaseFont {
    public DSFont(@NotNull RTTIObject object) {
        super(object);
    }

    @Override
    public int getGlyphCount() {
        return data.objs("CodePointInfo").length;
    }

    @NotNull
    @Override
    public Glyph getGlyph(int index) {
        return new DSGlyph(data.objs("CodePointInfo")[index]);
    }

    private static class DSGlyph extends AbstractGlyph {
        public DSGlyph(@NotNull RTTIObject object) {
            super(object);
        }

        @Override
        public int getCodePoint() {
            return object.i32("CodePoint");
        }
    }
}
