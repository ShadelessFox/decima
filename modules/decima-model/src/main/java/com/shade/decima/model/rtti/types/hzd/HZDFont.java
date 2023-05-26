package com.shade.decima.model.rtti.types.hzd;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.types.base.BaseFont;
import com.shade.util.NotNull;

public class HZDFont extends BaseFont {
    public HZDFont(@NotNull RTTIObject object) {
        super(object);
    }

    @Override
    public int getGlyphCount() {
        return data.objs("CharInfo").length;
    }

    @NotNull
    @Override
    public Glyph getGlyph(int index) {
        return new HZDGlyph(data.objs("CharInfo")[index]);
    }

    private static class HZDGlyph extends AbstractGlyph {
        public HZDGlyph(@NotNull RTTIObject object) {
            super(object);
        }

        @Override
        public int getCodePoint() {
            return object.i16("Char") & 0xffff;
        }
    }
}
