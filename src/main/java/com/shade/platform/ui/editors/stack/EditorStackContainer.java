package com.shade.platform.ui.editors.stack;

import com.shade.platform.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.platform.ui.editors.EditorChangeListener;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Represent a host for one or more editor stacks.
 * <p>
 * Can contain either a single {@link EditorStack} or a {@link JSplitPane} with two {@link EditorStackContainer}s.
 */
public class EditorStackContainer extends JComponent {
    private final EditorStackManager manager;

    public EditorStackContainer(@NotNull EditorStackManager manager, @Nullable Component component) {
        this.manager = manager;

        if (component == null) {
            component = new EditorStack(manager);
            manager.fireEditorChangeEvent(EditorChangeListener::editorStackCreated, (EditorStack) component);
        }

        setLayout(new BorderLayout());
        add(component, BorderLayout.CENTER);
    }

    @NotNull
    public SplitResult split(int orientation, double position, boolean leading) {
        final var first = new EditorStackContainer(manager, getComponent(0));
        final var second = new EditorStackContainer(manager, null);

        final JSplitPane pane = new JSplitPane(orientation);
        pane.setUI(new ThinFlatSplitPaneUI());
        pane.setLeftComponent(leading ? second : first);
        pane.setRightComponent(leading ? first : second);
        pane.setResizeWeight(position);
        pane.setDividerLocation((int) ((orientation == JSplitPane.HORIZONTAL_SPLIT ? getWidth() : getHeight()) * position));

        removeAll();
        add(pane, BorderLayout.CENTER);

        invalidate();
        validate();
        repaint();

        return new SplitResult(first, second);
    }

    public void compact() {
        Component leaf = getComponent(0);

        if (leaf instanceof JSplitPane pane) {
            final Component left = pane.getLeftComponent();
            final Component right = pane.getRightComponent();

            if (canCompact(left)) {
                leaf = right;
            } else if (canCompact(right)) {
                leaf = left;
            }
        }

        while (leaf instanceof EditorStackContainer container) {
            leaf = container.getComponent(0);
        }

        if (leaf != null) {
            removeAll();
            add(leaf, BorderLayout.CENTER);

            revalidate();
            invalidate();
        }

        final EditorStackContainer parent = (EditorStackContainer) SwingUtilities.getAncestorOfClass(EditorStackContainer.class, this);

        if (parent != null) {
            parent.compact();
        }
    }

    public boolean isSplit() {
        return getComponent(0) instanceof JSplitPane;
    }

    @NotNull
    public EditorStackContainer getLeftContainer() {
        return (EditorStackContainer) ((JSplitPane) getComponent(0)).getLeftComponent();
    }

    @NotNull
    public EditorStackContainer getRightContainer() {
        return (EditorStackContainer) ((JSplitPane) getComponent(0)).getRightComponent();
    }

    public double getSplitPosition() {
        if (getComponent(0) instanceof JSplitPane pane) {
            return (double) pane.getDividerLocation() / (pane.getOrientation() == JSplitPane.VERTICAL_SPLIT ? pane.getHeight() : pane.getWidth());
        } else {
            throw new IllegalStateException("Container is not split");
        }
    }

    public int getSplitOrientation() {
        if (getComponent(0) instanceof JSplitPane pane) {
            return pane.getOrientation();
        } else {
            throw new IllegalStateException("Container is not split");
        }
    }

    public int getSelectionIndex() {
        if (getComponent(0) instanceof JTabbedPane pane) {
            return pane.getSelectedIndex();
        } else {
            throw new IllegalStateException("Container is not an editor stack");
        }
    }

    @NotNull
    public Component[] getChildren() {
        final Component component = getComponent(0);

        if (component instanceof JTabbedPane pane) {
            final Component[] children = new Component[pane.getTabCount()];

            for (int i = 0; i < children.length; i++) {
                children[i] = pane.getComponentAt(i);
            }

            return children;
        } else {
            final JSplitPane pane = (JSplitPane) component;

            return new Component[]{
                pane.getLeftComponent(),
                pane.getRightComponent()
            };
        }
    }

    private static boolean canCompact(@NotNull Component component) {
        if (component instanceof JSplitPane pane) {
            return canCompact(pane.getLeftComponent()) && canCompact(pane.getRightComponent());
        } else if (component instanceof EditorStackContainer pane) {
            return canCompact(pane.getComponent(0));
        } else {
            return ((EditorStack) component).getTabCount() == 0;
        }
    }

    public record SplitResult(@NotNull EditorStackContainer leading, @NotNull EditorStackContainer trailing) {
        @NotNull
        public EditorStack targetStack() {
            return (EditorStack) trailing.getComponent(0);
        }
    }
}
