package com.shade.decima.ui.controls.hex.panel;

import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.util.NotNull;

import java.awt.*;

public class HexPanelMain extends HexPanel {
    private static final Decorator DECORATOR = new Decorator() {
        @Override
        public boolean isGrayed(byte value) {
            return value == 0;
        }

        @NotNull
        @Override
        public String toString(byte value) {
            return toHexDigit(value);
        }
    };

    public HexPanelMain(@NotNull HexEditor editor) {
        super(editor);

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    @Override
    protected void doPaint(@NotNull Graphics2D g, int startIndex, int endIndex) {
        doPaintBackground(g, startIndex, endIndex, true);
        doPaintCaret(g, startIndex, endIndex);
        doPaintData(g, startIndex, endIndex, DECORATOR);
    }

    @Override
    public int getColumnWidth() {
        return super.getColumnWidth() * 3;
    }

    @Override
    protected int getColumnInsets() {
        return editor.getColumnWidth() / 2;
    }
}
