package com.shade.platform.ui.controls;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class ColoredListCellRenderer<T> extends ColoredComponent implements ListCellRenderer<T> {
    private static final Insets INSETS_LIST = new Insets(2, 6, 2, 6);
    private static final Insets INSETS_COMBO = new Insets(0, 6, 0, 6);

    private boolean selected;

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

        if (list.getModel() instanceof ComboBoxModel<?>) {
            setPadding(INSETS_COMBO);
        } else {
            setPadding(INSETS_LIST);
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
