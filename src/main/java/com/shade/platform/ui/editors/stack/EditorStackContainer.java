package com.shade.platform.ui.editors.stack;

import com.shade.platform.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Represent a host for one or more editor stacks.
 * <p>
 * Can contain either a single {@link EditorStack} or a {@link JSplitPane} with two {@link EditorStackContainer}s.
 */
public class EditorStackContainer extends JComponent {
    public EditorStackContainer(@NotNull Component component) {
        setLayout(new BorderLayout());
        add(component, BorderLayout.CENTER);
    }

    public EditorStackContainer() {
        setLayout(new BorderLayout());
        add(createEditorStack(), BorderLayout.CENTER);
    }

    @NotNull
    public EditorStack split(int orientation, boolean leading) {
        final var first = new EditorStackContainer(getComponent(0));
        final var second = new EditorStackContainer();

        final JSplitPane pane = new JSplitPane(orientation);
        pane.setUI(new ThinFlatSplitPaneUI());
        pane.setLeftComponent(leading ? second : first);
        pane.setRightComponent(leading ? first : second);
        pane.setResizeWeight(0.5);

        removeAll();
        add(pane, BorderLayout.CENTER);

        revalidate();
        invalidate();

        return (EditorStack) second.getComponent(0);
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

    @NotNull
    protected EditorStack createEditorStack() {
        return new EditorStack();
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
}
