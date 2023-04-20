package com.shade.platform.ui.controls;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class ColoredListCellRenderer<T> extends ColoredComponent implements ListCellRenderer<T> {
    private boolean selected;

    public ColoredListCellRenderer() {
        setPadding(new Insets(0, 6, 0, 6) /* List.cellNoFocusBorder */);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean selected, boolean focused) {
        clear();

        this.selected = selected;

        if (selected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        customizeCellRenderer(list, value, index, selected, focused);

        final Icon leadingIcon = getLeadingIcon();
        final Icon trailingIcon = getTrailingIcon();

        if ((leadingIcon != null || trailingIcon != null) && selected) {
            final FlatSVGIcon.ColorFilter selectionFilter = UIUtils.createSelectionColorFilter(list.getSelectionBackground());
            if (leadingIcon != null) {
                setLeadingIcon(UIUtils.applyColorFilter(leadingIcon, selectionFilter));
            }
            if (trailingIcon != null) {
                setTrailingIcon(UIUtils.applyColorFilter(trailingIcon, selectionFilter));
            }
        }

        return this;
    }

    @Override
    public void append(@NotNull String fragment, @NotNull TextAttributes attributes) {
        if (selected) {
            super.append(fragment, new TextAttributes(getForeground(), attributes.styles()));
        } else {
            super.append(fragment, attributes);
        }
    }

    protected abstract void customizeCellRenderer(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean focused);
}
