package com.shade.platform.ui.editors.stack;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorChangeListener;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

import static com.shade.platform.ui.PlatformDataKeys.EDITOR_KEY;

/**
 * Represents a stack of editors grouped together.
 */
public class EditorStack extends JTabbedPane {
    private static final int[] POSITIONS = {
        SwingConstants.CENTER,
        SwingConstants.NORTH,
        SwingConstants.EAST,
        SwingConstants.SOUTH,
        SwingConstants.WEST
    };

    private int splitPosition = -1;
    private int dropIndex = -1;

    public EditorStack() {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close");
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (IntConsumer) index -> {
            final JComponent component = (JComponent) getComponentAt(index);
            final Editor editor = EDITOR_KEY.get(component);
            getManager().closeEditor(editor);
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
                    final int index = indexAtLocation(e.getX(), e.getY());

                    if (index >= 0) {
                        final JComponent component = (JComponent) getComponentAt(index);
                        final Editor editor = EDITOR_KEY.get(component);
                        getManager().closeEditor(editor);
                    }
                }
            }
        });

        addChangeListener(e -> {
            final EditorStackManager manager = getManager();

            manager.fireEditorChangeEvent(EditorChangeListener::editorChanged, manager.getActiveEditor());

            if (getTabCount() == 0) {
                getContainer().compact();

                if (manager.getLastEditorStack() == this) {
                    manager.setLastEditorStack(null);
                }
            } else {
                manager.setLastEditorStack(this);
            }
        });

        DragSource
            .getDefaultDragSource()
            .createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, new TabDragSourceListener());

        new DropTarget(this, DnDConstants.ACTION_MOVE, new TabDropTargetListener());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (getTabCount() == 0 || splitPosition < 0 && dropIndex < 0) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.4f));
            g2.setColor(Color.BLACK);

            if (splitPosition >= 0) {
                final Rectangle bounds = getComponentAt(0).getBounds();
                g2.fillPolygon(getDropVisualPolygon(bounds, splitPosition));
            } else if (dropIndex == getTabCount()) {
                final Rectangle bounds = getBoundsAt(dropIndex - 1);
                g2.fillRect(bounds.x + bounds.width - 2, bounds.y, 2, bounds.height + 1);
            } else if (dropIndex >= 0) {
                final Rectangle bounds = getBoundsAt(dropIndex);
                g2.fillRect(bounds.x, bounds.y, 2, bounds.height + 1);
            }
        } finally {
            g2.dispose();
        }
    }

    public boolean split(@NotNull EditorStack sourceStack, @NotNull Editor sourceEditor, int targetPosition) {
        for (int i = 0; i < sourceStack.getTabCount(); i++) {
            final JComponent component = (JComponent) sourceStack.getComponentAt(i);
            final Editor editor = EDITOR_KEY.get(component);

            if (editor == sourceEditor) {
                return split(sourceStack, i, targetPosition);
            }
        }

        return false;
    }

    private boolean split(@NotNull EditorStack sourceStack, int sourceIndex, int targetPosition) {
        if (targetPosition != SwingConstants.CENTER &&
            targetPosition != SwingConstants.NORTH &&
            targetPosition != SwingConstants.SOUTH &&
            targetPosition != SwingConstants.WEST &&
            targetPosition != SwingConstants.EAST
        ) {
            throw new IllegalArgumentException("Invalid position: " + targetPosition);
        }

        if (sourceStack == this && getTabCount() < 2) {
            return false;
        }

        final EditorStack targetStack;
        final int targetIndex;

        if (targetPosition == SwingConstants.CENTER) {
            targetStack = this;
            targetIndex = getTabCount();
        } else {
            final int orientation;
            final boolean leading;

            if (targetPosition == SwingConstants.NORTH || targetPosition == SwingConstants.SOUTH) {
                orientation = SwingConstants.HORIZONTAL;
                leading = targetPosition == SwingConstants.NORTH;
            } else {
                orientation = SwingConstants.VERTICAL;
                leading = targetPosition == SwingConstants.WEST;
            }

            targetStack = getContainer().split(orientation, leading);
            targetIndex = 0;
        }

        return targetStack.move(sourceStack, sourceIndex, targetIndex);
    }

    private boolean move(@NotNull EditorStack sourceStack, int sourceIndex, int targetIndex) {
        final var title = sourceStack.getTitleAt(sourceIndex);
        final var icon = sourceStack.getIconAt(sourceIndex);
        final var component = sourceStack.getComponentAt(sourceIndex);
        final var tip = sourceStack.getToolTipTextAt(sourceIndex);

        if (sourceStack != this) {
            sourceStack.remove(sourceIndex);

            if (targetIndex == getTabCount()) {
                addTab(title, icon, component, tip);
            } else {
                insertTab(title, icon, component, tip, targetIndex);
            }

            setSelectedIndex(targetIndex);
        } else if (sourceIndex != targetIndex && targetIndex >= 0) {
            if (targetIndex == getTabCount()) {
                if (getTabCount() > 1) {
                    sourceStack.remove(sourceIndex);
                    addTab(title, icon, component, tip);
                    setSelectedIndex(getTabCount() - 1);
                }
            } else if (sourceIndex > targetIndex) {
                sourceStack.remove(sourceIndex);
                insertTab(title, icon, component, tip, targetIndex);
                setSelectedIndex(targetIndex);
            } else {
                sourceStack.remove(sourceIndex);
                insertTab(title, icon, component, tip, targetIndex - 1);
                setSelectedIndex(targetIndex - 1);
            }
        } else {
            return false;
        }

        sourceStack.getContainer().compact();

        return true;
    }

    private void updateVisuals(@Nullable Point point, boolean sourceIsTarget) {
        splitPosition = -1;
        dropIndex = -1;

        final int tabs = getTabCount();

        if (point != null && tabs > 0) {
            final Rectangle bounds = getComponentAt(0).getBounds();

            if (!bounds.contains(point)) {
                dropIndex = getTargetTabIndex(this, point, sourceIsTarget);
            } else if (tabs == 1 && sourceIsTarget) {
                splitPosition = SwingConstants.CENTER;
            } else {
                for (int position : POSITIONS) {
                    if (getDropHoverPolygon(bounds, position).contains(point)) {
                        splitPosition = position;
                        break;
                    }
                }
            }

        }

        repaint();
    }

    private static int getTargetTabIndex(@NotNull JTabbedPane target, @NotNull Point point, boolean sourceIsTarget) {
        final int tabs = target.getTabCount();

        if (tabs == 0 || (tabs == 1 && sourceIsTarget)) {
            return 0;
        }

        final boolean isTopOrBottom = target.getTabPlacement() == JTabbedPane.TOP || target.getTabPlacement() == JTabbedPane.BOTTOM;

        for (int i = 0; i < tabs; i++) {
            final Rectangle bounds = target.getBoundsAt(i);

            if (isTopOrBottom) {
                bounds.setRect(bounds.x - bounds.width / 2.0, bounds.y, bounds.width, bounds.height);
            } else {
                bounds.setRect(bounds.x, bounds.y - bounds.height / 2.0, bounds.width, bounds.height);
            }

            if (bounds.contains(point)) {
                return i;
            }
        }

        final Rectangle bounds = target.getBoundsAt(tabs - 1);

        if (isTopOrBottom) {
            final int x = bounds.x + bounds.width / 2;
            bounds.setRect(x, bounds.y, target.getWidth() - x, bounds.height);
        } else {
            final int y = bounds.y + bounds.height / 2;
            bounds.setRect(bounds.x, y, bounds.width, target.getHeight() - y);
        }

        if (bounds.contains(point)) {
            return tabs;
        }

        return -1;
    }

    @NotNull
    private static Polygon getDropHoverPolygon(@NotNull Rectangle b, int position) {
        return switch (position) {
            case SwingConstants.NORTH -> new Polygon(
                new int[]{b.x, b.x + b.width / 2, b.x + b.width},
                new int[]{b.y, b.y + b.height / 2, b.y},
                3
            );
            case SwingConstants.EAST -> new Polygon(
                new int[]{b.x + b.width, b.x + b.width / 2, b.x + b.width},
                new int[]{b.y, b.y + b.height / 2, b.y + b.height - 2},
                3
            );
            case SwingConstants.SOUTH -> new Polygon(
                new int[]{b.x, b.x + b.width / 2, b.x + b.width},
                new int[]{b.y + b.height, b.y + b.height / 2, b.y + b.height},
                3
            );
            case SwingConstants.WEST -> new Polygon(
                new int[]{b.x, b.x + b.width / 2, b.x},
                new int[]{b.y, b.y + b.height / 2, b.y + b.height - 2},
                3
            );
            case CENTER -> new Polygon(
                new int[]{b.x + b.width / 4, b.x + b.width - b.width / 4, b.x + b.width - b.width / 4, b.x + b.width / 4},
                new int[]{b.y + b.height / 4, b.y + b.height / 4, b.y + b.height - b.height / 4, b.y + b.height - b.height / 4},
                4
            );
            default -> throw new IllegalArgumentException("Invalid position: " + position);
        };
    }

    @NotNull
    private static Polygon getDropVisualPolygon(@NotNull Rectangle b, int position) {
        return switch (position) {
            case SwingConstants.NORTH -> new Polygon(
                new int[]{b.x, b.x + b.width, b.x + b.width, b.x},
                new int[]{b.y, b.y, b.y + b.height / 2, b.y + b.height / 2},
                4
            );
            case SwingConstants.EAST -> new Polygon(
                new int[]{b.x + b.width / 2, b.x + b.width, b.x + b.width, b.x + b.width / 2},
                new int[]{b.y, b.y, b.y + b.height - 1, b.y + b.height - 1},
                4
            );
            case SwingConstants.SOUTH -> new Polygon(
                new int[]{b.x, b.x + b.width, b.x + b.width, b.x},
                new int[]{b.y + b.height / 2, b.y + b.height / 2, b.y + b.height - 1, b.y + b.height - 1},
                4
            );
            case SwingConstants.WEST -> new Polygon(
                new int[]{b.x, b.x + b.width / 2, b.x + b.width / 2, b.x},
                new int[]{b.y, b.y, b.y + b.height - 1, b.y + b.height - 1},
                4
            );
            case CENTER -> new Polygon(
                new int[]{b.x, b.x + b.width, b.x + b.width, b.x},
                new int[]{b.y, b.y, b.y + b.height - 1, b.y + b.height - 1},
                4
            );
            default -> throw new IllegalArgumentException("Invalid position: " + position);
        };
    }

    @NotNull
    private EditorStackContainer getContainer() {
        return ((EditorStackContainer) getParent());
    }

    @NotNull
    private EditorStackManager getManager() {
        return (EditorStackManager) SwingUtilities.getAncestorOfClass(EditorStackManager.class, this);
    }

    private class TabDragSourceListener extends DragSourceAdapter implements DragGestureListener {
        @Override
        public void dragGestureRecognized(DragGestureEvent event) {
            if (event.getTriggerEvent() instanceof MouseEvent mouse && mouse.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
                final int index = indexAtLocation(mouse.getX(), mouse.getY());

                if (index >= 0) {
                    final Transferable transferable = new TabTransferable(EditorStack.this, index);
                    final DragSource source = event.getDragSource();

                    source.startDrag(event, null, transferable, this, null);
                }
            }
        }
    }

    private class TabDropTargetListener extends DropTargetAdapter {
        @Override
        public void dragExit(DropTargetEvent dte) {
            updateVisuals(null, false);
        }

        @Override
        public void dragOver(DropTargetDragEvent event) {
            if (event.isDataFlavorSupported(TabTransferable.FLAVOR)) {
                event.acceptDrag(DnDConstants.ACTION_MOVE);
            } else {
                event.rejectDrag();
                return;
            }

            final TabData source;

            try {
                source = (TabData) event.getTransferable().getTransferData(TabTransferable.FLAVOR);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            updateVisuals(event.getLocation(), source.stack() == EditorStack.this);
        }

        @Override
        public void drop(DropTargetDropEvent event) {
            final TabData source;
            final boolean success;

            try {
                source = (TabData) event.getTransferable().getTransferData(TabTransferable.FLAVOR);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (dropIndex >= 0) {
                success = move(source.stack(), source.index(), dropIndex);
            } else if (splitPosition >= 0) {
                success = split(source.stack(), source.index(), splitPosition);
            } else {
                success = false;
            }

            if (success) {
                event.acceptDrop(DnDConstants.ACTION_MOVE);
                event.dropComplete(true);
            } else {
                event.rejectDrop();
                event.dropComplete(false);
            }

            updateVisuals(null, false);
        }
    }

    private static record TabTransferable(@NotNull TabData data) implements Transferable {
        public static final DataFlavor FLAVOR;

        static {
            try {
                FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + TabData.class.getName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can't create data flavor", e);
            }
        }

        public TabTransferable(@NotNull EditorStack stack, int index) {
            this(new TabData(stack, index));
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return data;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    private static record TabData(@NotNull EditorStack stack, int index) {}
}
