package com.shade.decima.ui.controls.hex;

import com.shade.decima.ui.controls.hex.impl.DefaultHexCaret;
import com.shade.decima.ui.controls.hex.impl.DefaultHexModel;
import com.shade.decima.ui.controls.hex.panel.*;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.*;

public class HexEditor extends JComponent implements Scrollable {
    public static final Color COLOR_FOREGROUND = UIManager.getColor("HexEditor.foreground");
    public static final Color COLOR_SELECTION_FOREGROUND = UIManager.getColor("HexEditor.selectionForeground");

    public static final Color COLOR_BACKGROUND = UIManager.getColor("HexEditor.background");
    public static final Color COLOR_ODD_BACKGROUND = UIManager.getColor("HexEditor.oddBackground");
    public static final Color COLOR_HOT_BACKGROUND = UIManager.getColor("HexEditor.hotBackground");
    public static final Color COLOR_SELECTION_BACKGROUND = UIManager.getColor("HexEditor.selectionBackground");
    public static final Color COLOR_SELECTION_INACTIVE_BACKGROUND = UIManager.getColor("HexEditor.selectionInactiveBackground");

    public static final Color COLOR_CARET_FOREGROUND = UIManager.getColor("HexEditor.caretForeground");
    public static final Color COLOR_CARET_INACTIVE_BACKGROUND = UIManager.getColor("HexEditor.caretInactiveBackground");

    public static final Color COLOR_DIVIDER_FOREGROUND = UIManager.getColor("HexEditor.dividerForeground");
    public static final Color COLOR_DIVIDER_SELECTION_FOREGROUND = UIManager.getColor("HexEditor.dividerSelectionForeground");


    private HexModel model;
    private HexCaret caret;
    private Font boldFont;
    private int dividerSize;
    private int rowLength;
    private int columnWidth;
    private int rowHeight;

    private final HexPanel rowsPanel;
    private final HexPanel colsPanel;
    private final HexPanel mainPanel;
    private final HexPanel textPanel;

    public HexEditor(@NotNull HexModel model) {
        setModel(model);
        setCaret(new DefaultHexCaret());
        setDividerSize(4);
        setRowLength(16);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIUtils.getDefaultFontSize()));
        setAutoscrolls(true);
        setFocusable(true);

        final ActionMap am = getActionMap();
        am.put(Action.SELECT_PREVIOUS_COLUMN, new Action(Action.SELECT_PREVIOUS_COLUMN));
        am.put(Action.SELECT_PREVIOUS_COLUMN_EXTEND, new Action(Action.SELECT_PREVIOUS_COLUMN_EXTEND));
        am.put(Action.SELECT_NEXT_COLUMN, new Action(Action.SELECT_NEXT_COLUMN));
        am.put(Action.SELECT_NEXT_COLUMN_EXTEND, new Action(Action.SELECT_NEXT_COLUMN_EXTEND));
        am.put(Action.SELECT_PREVIOUS_ROW, new Action(Action.SELECT_PREVIOUS_ROW));
        am.put(Action.SELECT_PREVIOUS_ROW_EXTEND, new Action(Action.SELECT_PREVIOUS_ROW_EXTEND));
        am.put(Action.SELECT_NEXT_ROW, new Action(Action.SELECT_NEXT_ROW));
        am.put(Action.SELECT_NEXT_ROW_EXTEND, new Action(Action.SELECT_NEXT_ROW_EXTEND));
        am.put(Action.SELECT_CANCEL, new Action(Action.SELECT_CANCEL));
        am.put(Action.SELECT_ALL, new Action(Action.SELECT_ALL));
        am.put(Action.COPY, new Action(Action.COPY));

        final InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStroke.getKeyStroke("LEFT"), Action.SELECT_PREVIOUS_COLUMN);
        im.put(KeyStroke.getKeyStroke("shift LEFT"), Action.SELECT_PREVIOUS_COLUMN_EXTEND);
        im.put(KeyStroke.getKeyStroke("RIGHT"), Action.SELECT_NEXT_COLUMN);
        im.put(KeyStroke.getKeyStroke("shift RIGHT"), Action.SELECT_NEXT_COLUMN_EXTEND);
        im.put(KeyStroke.getKeyStroke("UP"), Action.SELECT_PREVIOUS_ROW);
        im.put(KeyStroke.getKeyStroke("shift UP"), Action.SELECT_PREVIOUS_ROW_EXTEND);
        im.put(KeyStroke.getKeyStroke("DOWN"), Action.SELECT_NEXT_ROW);
        im.put(KeyStroke.getKeyStroke("shift DOWN"), Action.SELECT_NEXT_ROW_EXTEND);
        im.put(KeyStroke.getKeyStroke("ESCAPE"), Action.SELECT_CANCEL);
        im.put(KeyStroke.getKeyStroke("ctrl A"), Action.SELECT_ALL);
        im.put(KeyStroke.getKeyStroke("ctrl C"), Action.COPY);

        this.mainPanel = new HexPanelMain(this);
        this.textPanel = new HexPanelASCII(this);
        this.rowsPanel = new HexPanelRows(this);
        this.colsPanel = new HexPanelColumns(this);

        setLayout(new BorderLayout(2, 0));
        add(mainPanel, BorderLayout.CENTER);
        add(textPanel, BorderLayout.EAST);
        add(rowsPanel, BorderLayout.WEST);

        final Handler handler = new Handler();
        addMouseListener(handler);
        addFocusListener(handler);
    }

    public HexEditor() {
        this(new DefaultHexModel(new byte[0]));
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        boldFont = null;
        columnWidth = 0;
        rowHeight = 0;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        unconfigureEnclosingScrollPane();
    }

    public boolean isSelected(int index) {
        final int dot = caret.getDot();
        final int mark = caret.getMark();
        return mark != dot && (mark > dot ? dot <= index && index <= mark : mark <= index && index <= dot);
    }

    @NotNull
    public HexModel getModel() {
        return model;
    }

    public void setModel(@NotNull HexModel model) {
        if (this.model != model) {
            final HexModel oldModel = this.model;

            this.model = model;

            firePropertyChange("model", oldModel, model);

            revalidate();
            repaint();
        }
    }

    @NotNull
    public HexCaret getCaret() {
        return caret;
    }

    public void setCaret(@NotNull HexCaret caret) {
        if (this.caret != caret) {
            final HexCaret oldCaret = this.caret;

            this.caret = caret;

            firePropertyChange("caret", oldCaret, caret);
        }
    }

    public int getDividerSize() {
        return dividerSize;
    }

    public void setDividerSize(int dividerSize) {
        if (this.dividerSize != dividerSize) {
            final int oldDividerSize = this.dividerSize;

            this.dividerSize = dividerSize;

            firePropertyChange("dividerSize", oldDividerSize, dividerSize);
        }
    }

    public int getColumnWidth() {
        if (columnWidth == 0) {
            columnWidth = getFontMetrics(getFont()).charWidth('.');
        }

        return columnWidth;
    }

    @NotNull
    public Font getBoldFont() {
        if (boldFont == null) {
            boldFont = getFont().deriveFont(Font.BOLD);
        }

        return boldFont;
    }

    public int getRowHeight() {
        if (rowHeight == 0) {
            rowHeight = getFontMetrics(getFont()).getHeight();
        }

        return rowHeight;
    }

    public int getRowLength() {
        return rowLength;
    }

    public void setRowLength(int rowLength) {
        if (this.rowLength != rowLength) {
            final int oldRowLength = this.rowLength;

            this.rowLength = rowLength;

            firePropertyChange("rowLength", oldRowLength, rowLength);

            revalidate();
        }
    }

    public int getRowCount() {
        final int length = model.length();

        if (length > 0) {
            return (length - 1) / rowLength + 1;
        } else {
            return 0;
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return switch (orientation) {
            case SwingConstants.HORIZONTAL -> getColumnWidth();
            case SwingConstants.VERTICAL -> getRowHeight();
            default -> throw new IllegalArgumentException("Invalid orientation: " + orientation);
        };
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getScrollableUnitIncrement(visibleRect, orientation, direction) * 2;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void scrollCaretToVisible() {
        scrollRectToVisible(new Rectangle(
            getColumnWidth() * getColumnAt(caret.getDot()),
            getRowHeight() * getRowAt(caret.getDot()),
            getColumnWidth(),
            getRowHeight()
        ));
    }

    public void scrollSelectionToVisible() {
        final int dotCol = getColumnAt(caret.getDot());
        final int markCol = getColumnAt(caret.getMark());
        final int dotRow = getRowAt(caret.getDot());
        final int markRow = getRowAt(caret.getMark());

        scrollRectToVisible(new Rectangle(
            getColumnWidth() * Math.min(dotCol, markCol),
            getRowHeight() * Math.min(dotRow, markRow),
            getColumnWidth() * (Math.abs(dotCol - markCol) + 1),
            getRowHeight() * (Math.abs(dotRow - markRow) + 1)
        ));
    }

    public int getRowAt(int index) {
        return index / rowLength;
    }

    public int getColumnAt(int index) {
        return index % rowLength;
    }

    @NotNull
    public HexPanel getRowsPanel() {
        return rowsPanel;
    }

    @NotNull
    public HexPanel getColsPanel() {
        return colsPanel;
    }

    @NotNull
    public HexPanel getMainPanel() {
        return mainPanel;
    }

    @NotNull
    public HexPanel getTextPanel() {
        return textPanel;
    }

    private void configureEnclosingScrollPane() {
        final Container parent = SwingUtilities.getUnwrappedParent(this);

        if (parent instanceof JViewport port && port.getParent() instanceof JScrollPane scrollPane) {
            final JViewport viewport = scrollPane.getViewport();

            if (viewport != null && SwingUtilities.getUnwrappedView(viewport) == this) {
                scrollPane.setColumnHeaderView(colsPanel);
            }
        }
    }

    private void unconfigureEnclosingScrollPane() {
        final Container parent = SwingUtilities.getUnwrappedParent(this);

        if (parent instanceof JViewport port && port.getParent() instanceof JScrollPane scrollPane) {
            final JViewport viewport = scrollPane.getViewport();

            if (viewport != null && SwingUtilities.getUnwrappedView(viewport) == this) {
                scrollPane.setColumnHeaderView(null);
            }
        }
    }

    private class Action extends AbstractAction {
        private static final String SELECT_PREVIOUS_COLUMN = "selectPreviousColumn";
        private static final String SELECT_PREVIOUS_COLUMN_EXTEND = "selectPreviousColumnExtend";
        private static final String SELECT_NEXT_COLUMN = "selectNextColumn";
        private static final String SELECT_NEXT_COLUMN_EXTEND = "selectNextColumnExtend";
        private static final String SELECT_PREVIOUS_ROW = "selectPreviousRow";
        private static final String SELECT_PREVIOUS_ROW_EXTEND = "selectPreviousRowExtend";
        private static final String SELECT_NEXT_ROW = "selectNextRow";
        private static final String SELECT_NEXT_ROW_EXTEND = "selectNextRowExtend";
        private static final String SELECT_CANCEL = "selectCancel";
        private static final String SELECT_ALL = "selectAll";
        private static final String COPY = "copy";

        public Action(String name) {
            putValue(Action.ACTION_COMMAND_KEY, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int dot = caret.getDot();
            final int mark = caret.getMark();

            switch (e.getActionCommand()) {
                case SELECT_PREVIOUS_COLUMN -> caret.setDot(Math.max(dot - 1, 0));
                case SELECT_PREVIOUS_COLUMN_EXTEND -> caret.moveDot(Math.max(dot - 1, 0));
                case SELECT_NEXT_COLUMN -> caret.setDot(Math.min(dot + 1, model.length() - 1));
                case SELECT_NEXT_COLUMN_EXTEND -> caret.moveDot(Math.min(dot + 1, model.length() - 1));
                case SELECT_PREVIOUS_ROW -> caret.setDot(Math.max(dot - rowLength, 0));
                case SELECT_PREVIOUS_ROW_EXTEND -> caret.moveDot(Math.max(dot - rowLength, 0));
                case SELECT_NEXT_ROW -> caret.setDot(Math.min(dot + rowLength, model.length() - 1));
                case SELECT_NEXT_ROW_EXTEND -> caret.moveDot(Math.min(dot + rowLength, model.length() - 1));
                case SELECT_CANCEL -> caret.setDot(dot);
                case SELECT_ALL -> {
                    caret.setDot(0);
                    caret.moveDot(model.length() - 1);
                    return;
                }
                case COPY -> {
                    final int start = Math.min(dot, mark);
                    final int end = Math.max(dot, mark) + 1;
                    final int length = end - start;

                    if (length > 1) {
                        final byte[] data = new byte[length];
                        model.get(start, data, 0, length);

                        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        final ByteContents contents = new ByteContents(data);
                        clipboard.setContents(contents, contents);
                    }

                    return;
                }
            }

            scrollCaretToVisible();
        }
    }

    private class Handler extends MouseAdapter implements FocusListener {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
        }

        @Override
        public void focusGained(FocusEvent e) {
            // TODO: Repaint just the selection
            repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            focusGained(e);
        }
    }
}
