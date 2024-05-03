package com.shade.platform.ui.controls;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;

public abstract class ColoredTreeCellRenderer<T> extends ColoredComponent implements TreeCellRenderer {
    private JTree tree;
    private boolean selected;

    private Color foregroundSelectionColor;
    private Color foregroundNonSelectionColor;
    private Color backgroundSelectionColor;
    private Color backgroundNonSelectionColor;

    public ColoredTreeCellRenderer() {
        updateUI();
        setPadding(new Insets(2, 2, 2, 2));
    }

    @NotNull
    public ColoredTreeCellRenderer<T> withTags(@NotNull JTree tree) {
        new TreeTagMouseListener().installOn(tree);
        return this;
    }

    @Override
    public void updateUI() {
        super.updateUI();

        this.foregroundSelectionColor = UIManager.getColor("Tree.selectionForeground");
        this.foregroundNonSelectionColor = UIManager.getColor("Tree.textForeground");
        this.backgroundSelectionColor = UIManager.getColor("Tree.selectionBackground");
        this.backgroundNonSelectionColor = UIManager.getColor("Tree.textBackground");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
        clear();

        this.tree = tree;
        this.selected = selected;

        Icon icon = getIcon(tree, (T) value, selected, expanded, focused, leaf, row);

        if (icon != null && !tree.isEnabled()) {
            icon = Objects.requireNonNullElse(UIManager.getLookAndFeel().getDisabledIcon(tree, icon), icon);
        }

        setBackground(selected ? backgroundSelectionColor : backgroundNonSelectionColor);
        setForeground(selected ? foregroundSelectionColor : foregroundNonSelectionColor);
        setLeadingIcon(icon);
        setFont(tree.getFont());

        if (value instanceof TreeNode node && tree.getModel() instanceof TreeModel model && model.isPlaceholder(node)) {
            append(node.getLabel(), TextAttributes.GRAYED_ATTRIBUTES);
        } else {
            customizeCellRenderer(tree, (T) value, selected, expanded, focused, leaf, row);
        }

        return this;
    }

    @Override
    public void append(@NotNull String fragment, @NotNull TextAttributes attributes, @Nullable Tag tag) {
        if (selected && isFocused()) {
            super.append(fragment, new TextAttributes(getForeground(), attributes.styles()), tag);
        } else {
            super.append(fragment, attributes, tag);
        }
    }

    @Nullable
    public Icon getIcon(@NotNull JTree tree, @NotNull T value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof TreeNode node && !node.hasIcon()) {
            return null;
        }
        if (leaf) {
            return UIManager.getIcon("Tree.leafIcon");
        } else if (expanded) {
            return UIManager.getIcon("Tree.openIcon");
        } else {
            return UIManager.getIcon("Tree.closedIcon");
        }
    }

    protected final boolean isFocused() {
        return FlatUIUtils.isPermanentFocusOwner(tree);
    }

    protected abstract void customizeCellRenderer(@NotNull JTree tree, @NotNull T value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row);

    private class TreeTagMouseListener extends TagMouseListener<Tag> {
        private WeakReference<Object> lastHitNode;

        @Nullable
        @Override
        protected Tag getTagAt(@NotNull MouseEvent e) {
            final JTree tree = (JTree) e.getSource();
            final TreePath path = tree.getPathForLocation(e.getX(), e.getY());

            if (path != null) {
                final Object node = path.getLastPathComponent();

                if (lastHitNode == null || lastHitNode.get() != node || e.getButton() != MouseEvent.NOBUTTON) {
                    lastHitNode = new WeakReference<>(node);
                    getTreeCellRendererComponent(tree, node, false, false, tree.getModel().isLeaf(node), tree.getRowForPath(path), false);
                }

                return getFragmentTagAt(getRendererRelativeX(e, tree, path));
            }

            return null;
        }

        private int getRendererRelativeX(@NotNull MouseEvent e, @NotNull JTree tree, @NotNull TreePath path) {
            final Rectangle bounds = tree.getPathBounds(path);
            assert bounds != null;
            return e.getX() - bounds.x;
        }
    }
}
