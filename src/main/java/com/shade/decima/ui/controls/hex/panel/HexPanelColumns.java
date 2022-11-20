package com.shade.decima.ui.controls.hex.panel;

import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.util.NotNull;

import java.awt.*;

public class HexPanelColumns extends HexPanel {
    public HexPanelColumns(@NotNull HexEditor editor) {
        super(editor);
    }

    @Override
    protected void createListeners() {
        // do nothing
    }

    @Override
    protected void doPaint(@NotNull Graphics2D g, int startIndex, int endIndex) {
        doPaintBackground(g);
        doPaintData(g);
    }

    @Override
    protected int getColumnWidth() {
        return editor.getMainPanel().getColumnWidth() + editor.getTextPanel().getColumnWidth();
    }

    @Override
    protected int getColumnInsets() {
        return editor.getMainPanel().getColumnInsets() + editor.getTextPanel().getColumnInsets();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(editor.getRowLength() * getColumnWidth(), getRowHeight());
    }

    private void doPaintBackground(@NotNull Graphics2D g) {
        final Rectangle bounds = g.getClipBounds();

        g.setColor(HexEditor.COLOR_ROW_EVEN);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height - 1);

        g.setColor(HexEditor.COLOR_DIVIDER);
        g.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width, bounds.y + bounds.height - 1);
    }

    private void doPaintData(@NotNull Graphics2D g) {
        final int ascent = getFontMetrics(getFont()).getAscent();
        final int rowLength = editor.getRowLength();

        final HexPanel mainPanel = editor.getMainPanel();
        final HexPanel textPanel = editor.getTextPanel();
        final HexPanel rowsPanel = editor.getRowsPanel();

        final int mainX = rowsPanel.getWidth() + mainPanel.getColumnInsets();
        final int textX = mainPanel.getWidth() + mainX + 1;

        for (int i = 0; i < rowLength; i++) {
            final String digit = toHexDigit((byte) i);
            final boolean isHot = editor.getCaret().getDot() % rowLength == i;

            g.setFont(isHot ? editor.getBoldFont() : editor.getFont());
            g.setColor(HexEditor.COLOR_TEXT);
            g.drawString(digit, mainX + i * mainPanel.getColumnWidth() + mainPanel.getColumnInsets(), ascent);
            g.drawString(digit.substring(1), textX + i * textPanel.getColumnWidth() + textPanel.getColumnInsets(), ascent);
        }
    }
}
