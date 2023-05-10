package com.shade.decima.ui.controls.hex.panel;

import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.util.NotNull;

import java.awt.*;
import java.nio.charset.StandardCharsets;

public class HexPanelASCII extends HexPanel {
    private static final byte DISPLAYABLE_CHAR_START = ' ';
    private static final byte DISPLAYABLE_CHAR_END = '~';
    private static final byte DISPLAYABLE_CHAR_REPLACEMENT = '.';
    private static final byte[] DISPLAYABLE_CHARS;

    private static final Decorator DECORATOR = new Decorator() {
        @Override
        public boolean isGrayed(byte value) {
            return value < DISPLAYABLE_CHAR_START | value > DISPLAYABLE_CHAR_END;
        }

        @NotNull
        @Override
        public String toString(byte value) {
            return new String(DISPLAYABLE_CHARS, value & 0xff, 1, StandardCharsets.ISO_8859_1);
        }
    };

    static {
        DISPLAYABLE_CHARS = new byte[256];

        for (int i = 0; i < 256; i++) {
            DISPLAYABLE_CHARS[i] = (byte) (i >= DISPLAYABLE_CHAR_START && i <= DISPLAYABLE_CHAR_END ? i : DISPLAYABLE_CHAR_REPLACEMENT);
        }
    }

    public HexPanelASCII(@NotNull HexEditor editor) {
        super(editor);

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    @Override
    protected void doPaint(@NotNull Graphics2D g, int startIndex, int endIndex) {
        doPaintBackground(g, startIndex, endIndex, false);
        doPaintCaret(g, startIndex, endIndex);
        doPaintData(g, startIndex, endIndex, DECORATOR);
    }
}
