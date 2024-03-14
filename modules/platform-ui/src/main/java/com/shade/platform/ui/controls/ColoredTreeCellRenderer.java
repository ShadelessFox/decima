package com.shade.platform.ui.controls;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
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
    public void append(@NotNull String fragment, @NotNull TextAttributes attributes) {
        if (selected && isFocused()) {
            super.append(fragment, new TextAttributes(getForeground(), attributes.styles()));
        } else {
            super.append(fragment, attributes);
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
}
