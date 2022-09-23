package com.shade.platform.ui.controls;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.HSLColor;
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
    private FlatSVGIcon.ColorFilter selectionFilter;

    public ColoredTreeCellRenderer() {
        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        this.foregroundSelectionColor = UIManager.getColor("Tree.selectionForeground");
        this.foregroundNonSelectionColor = UIManager.getColor("Tree.textForeground");
        this.backgroundSelectionColor = UIManager.getColor("Tree.selectionBackground");
        this.backgroundNonSelectionColor = UIManager.getColor("Tree.textBackground");
        this.selectionFilter = new FlatSVGIcon.ColorFilter(color -> {
            final float[] bg = HSLColor.fromRGB(backgroundSelectionColor);
            final float[] fg = HSLColor.fromRGB(color);

            if (Math.abs(bg[0] - fg[0]) < 15.0f) {
                return HSLColor.toRGB(fg[0], fg[1], Math.min(fg[2] * 1.50f, 100.0f));
            } else {
                return HSLColor.toRGB(fg[0], fg[1], Math.min(fg[2] * 1.15f, 100.0f));
            }
        });
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

        if (icon instanceof FlatSVGIcon src && selected && isFocused()) {
            icon = getFocusedIcon(src);
        }

        setBackground(selected ? backgroundSelectionColor : backgroundNonSelectionColor);
        setForeground(selected ? foregroundSelectionColor : foregroundNonSelectionColor);
        setIcon(icon);
        setFont(tree.getFont());

        customizeCellRenderer(tree, (T) value, selected, expanded, focused, leaf, row);

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
        if (leaf) {
            return UIManager.getIcon("Tree.leafIcon");
        } else if (expanded) {
            return UIManager.getIcon("Tree.openIcon");
        } else {
            return UIManager.getIcon("Tree.closedIcon");
        }
    }

    @NotNull
    private FlatSVGIcon getFocusedIcon(@NotNull FlatSVGIcon icon) {
        final FlatSVGIcon filtered = new FlatSVGIcon(icon);
        filtered.setColorFilter(selectionFilter);
        return filtered;
    }

    protected final boolean isFocused() {
        return FlatUIUtils.isPermanentFocusOwner(tree);
    }

    protected abstract void customizeCellRenderer(@NotNull JTree tree, @NotNull T value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row);
}
