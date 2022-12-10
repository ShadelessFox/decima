package com.shade.decima.ui.controls.hex.panel;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.decima.ui.controls.hex.HexCaret;
import com.shade.decima.ui.controls.hex.HexEditor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class HexPanel extends JComponent implements ChangeListener {

    protected final HexEditor editor;

    public HexPanel(@NotNull HexEditor editor) {
        this.editor = editor;

        createListeners();
        setOpaque(true);

        editor.addPropertyChangeListener("caret", event -> {
            if (event.getOldValue() instanceof HexCaret caret) {
                caret.removeChangeListener(this);
            }
            if (event.getNewValue() instanceof HexCaret caret) {
                caret.addChangeListener(this);
            }
        });

        editor.getCaret().addChangeListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Rectangle clip = g.getClipBounds();
        final int startIndex = getClosestIndexAt(clip.x, clip.y);
        final int endIndex = getClosestIndexAt(clip.x + clip.width, clip.y + clip.height);

        if (startIndex == endIndex) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, UIManager.get(RenderingHints.KEY_TEXT_ANTIALIASING));
            g2.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, UIManager.get(RenderingHints.KEY_TEXT_LCD_CONTRAST));

            doPaint(g2, startIndex, endIndex);
        } finally {
            g2.dispose();
        }
    }

    protected void createListeners() {
        final Handler handler = new Handler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    protected abstract void doPaint(@NotNull Graphics2D g, int startIndex, int endIndex);

    protected void doPaintCaret(@NotNull Graphics2D g, int startIndex, int endIndex) {
        final int dot = editor.getCaret().getDot();

        if (startIndex > dot || dot > endIndex) {
            return;
        }

        final int rowLength = editor.getRowLength();
        final int x = dot % rowLength * getColumnWidth();
        final int y = dot / rowLength * getRowHeight();

        if (isFocused()) {
            g.setColor(HexEditor.COLOR_CARET);
            g.drawRect(x, y, getColumnWidth() - 1, getRowHeight() - 1);
        } else {
            g.setColor(HexEditor.COLOR_CARET_INACTIVE);
            g.fillRect(x, y, getColumnWidth(), getRowHeight());
        }
    }

    protected void doPaintBackground(@NotNull Graphics2D g, int startIndex, int endIndex, boolean paintDividers) {
        final int rowLength = editor.getRowLength();
        final HexCaret caret = editor.getCaret();
        final int dotLineStart = caret.getDot() - caret.getDot() % rowLength;
        final int dotLineEnd = dotLineStart + rowLength;
        final int rowLineEnd = endIndex + rowLength - endIndex % rowLength;

        for (int i = startIndex; i <= rowLineEnd; i++) {
            final int x = i % rowLength * getColumnWidth();
            final int y = i / rowLength * getRowHeight();

            final boolean isWithinData = i <= endIndex;
            final boolean isSelected = isWithinData && editor.isSelected(i);
            final boolean isHot = dotLineStart <= i && i < dotLineEnd;
            final boolean isEven = y % 2 == 0;
            final boolean isFocused = isFocused();

            g.setColor(isSelected ? isFocused ? HexEditor.COLOR_SELECTION : HexEditor.COLOR_SELECTION_INACTIVE : isHot ? HexEditor.COLOR_ROW_HOT : isEven ? HexEditor.COLOR_ROW_EVEN : HexEditor.COLOR_ROW_ODD);
            g.fillRect(x, y, getColumnWidth(), getRowHeight());

            if (paintDividers && isWithinData && x > 0 && x % editor.getDividerSize() == 0) {
                g.setColor(isSelected && isFocused ? HexEditor.COLOR_DIVIDER_SELECTION : HexEditor.COLOR_DIVIDER);
                g.drawLine(x, y, x, y + getRowHeight());
            }
        }
    }

    protected void doPaintData(@NotNull Graphics2D g, int startIndex, int endIndex, @NotNull Decorator decorator) {
        final int ascent = getFontMetrics(getFont()).getAscent();
        final int rowLength = editor.getRowLength();

        for (int i = startIndex; i <= endIndex; i++) {
            final int x = i % rowLength * getColumnWidth() + getColumnInsets();
            final int y = i / rowLength * getRowHeight();

            final byte value = editor.getModel().get(i);
            final boolean isGrayed = decorator.isGrayed(value);
            final boolean isSelected = isFocused() && editor.isSelected(i);
            final Color selectedColor = isSelected ? HexEditor.COLOR_TEXT_SELECTION : HexEditor.COLOR_TEXT;
            final Color unselectedColor = isSelected ? HexEditor.COLOR_TEXT : HexEditor.COLOR_TEXT_SELECTION;

            g.setColor(UIUtils.mix(selectedColor, unselectedColor, isGrayed ? 0.1f : 0.0f));
            g.drawString(decorator.toString(value), x, y + ascent);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(editor.getRowLength() * getColumnWidth(), editor.getRowCount() * getRowHeight());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        final Rectangle rect = getVisibleRect();
        repaint(rect.x, rect.y, rect.width, rect.height);
    }

    protected int getColumnWidth() {
        return editor.getColumnWidth();
    }

    protected int getRowHeight() {
        return editor.getRowHeight();
    }

    protected int getColumnInsets() {
        return 0;
    }

    protected int getClosestIndexAt(int x, int y) {
        final int col = Math.min(Math.max(x, 0), getWidth() - 1) / getColumnWidth();
        final int row = Math.min(Math.max(y, 0), getHeight() - 1) / getRowHeight();
        return Math.min(row * editor.getRowLength() + col, editor.getModel().length() - 1);
    }

    protected final boolean isFocused() {
        return FlatUIUtils.isPermanentFocusOwner(editor);
    }

    protected interface Decorator {
        boolean isGrayed(byte value);

        @NotNull
        String toString(byte value);
    }

    private class Handler extends MouseAdapter {
        private int origin = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                origin = getClosestIndexAt(e.getX(), e.getY());
            }

            if (origin < 0) {
                return;
            }

            if (e.isShiftDown()) {
                editor.getCaret().moveDot(origin);
            } else {
                editor.getCaret().setDot(origin);
            }

            editor.requestFocusInWindow();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            origin = -1;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin >= 0) {
                editor.getCaret().moveDot(getClosestIndexAt(e.getX(), e.getY()));
                scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
            }
        }
    }
}
