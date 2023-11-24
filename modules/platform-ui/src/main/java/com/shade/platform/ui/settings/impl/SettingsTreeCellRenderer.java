package com.shade.platform.ui.settings.impl;

import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeCellRenderer;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.awt.*;

public class SettingsTreeCellRenderer extends TreeCellRenderer {
    private final SettingsDialog dialog;

    public SettingsTreeCellRenderer(@NotNull SettingsDialog dialog) {
        this.dialog = dialog;

        setPadding(new Insets(3, 2, 3, 2));
    }

    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof SettingsTreeNodePage page) {
            TextAttributes attributes;

            if (!dialog.isComplete(page.getMetadata())) {
                attributes = CommonTextAttributes.ERROR_ATTRIBUTES;
            } else if (dialog.isModified(page.getMetadata())) {
                attributes = CommonTextAttributes.MODIFIED_ATTRIBUTES;
            } else {
                attributes = TextAttributes.REGULAR_ATTRIBUTES;
            }

            if (page.getMetadata().parent().isEmpty()) {
                // Make root pages bold
                attributes = attributes.bold();
            }

            append(value.getLabel(), attributes);
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        return null;
    }
}
