package com.shade.decima.ui.controls.hex.panel;

import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.awt.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class HexPanelRows extends HexPanel {
    private final byte[] buffer = new byte[8];
    private int digits;

    public HexPanelRows(@NotNull HexEditor editor) {
        super(editor);
    }

    @Override
    protected void createListeners() {
        // do nothing
    }

    @Override
    protected void doPaint(@NotNull Graphics2D g, int startIndex, int endIndex) {
        doPaintBackground(g);
        doPaintData(g, startIndex, endIndex);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getDigits() * getColumnWidth() + getColumnWidth(), editor.getRowCount() * getRowHeight());
    }

    private void doPaintBackground(@NotNull Graphics2D g) {
        final Rectangle bounds = g.getClipBounds();

        g.setColor(HexEditor.COLOR_ROW_EVEN);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void doPaintData(@NotNull Graphics2D g, int startIndex, int endIndex) {
        final int ascent = getFontMetrics(getFont()).getAscent();
        final int prefix = 8 - getDigits();

        final int hotRow = editor.getRowAt(editor.getCaret().getDot());
        final int startRow = editor.getRowAt(startIndex);
        final int endRow = editor.getRowAt(endIndex);

        for (int i = startRow; i <= endRow; i++) {
            final boolean isHot = hotRow == i;

            IOUtils.toHexDigits(i * editor.getRowLength(), buffer, 0, ByteOrder.BIG_ENDIAN);

            g.setFont(isHot ? editor.getBoldFont() : editor.getFont());
            g.setColor(HexEditor.COLOR_TEXT);
            g.drawString(new String(buffer, prefix, 8 - prefix, StandardCharsets.ISO_8859_1), getColumnWidth() / 2, i * getRowHeight() + ascent);
        }
    }

    private int getDigits() {
        if (digits == 0) {
            final int length = editor.getModel().getLength();

            if (length > 0) {
                digits = (int) Math.ceil(Math.log(length) / Math.log(16));
            }

            if (digits < 2) {
                digits = 2;
            }
        }

        return digits;
    }
}
