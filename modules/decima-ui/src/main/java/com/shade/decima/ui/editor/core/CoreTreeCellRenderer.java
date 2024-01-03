package com.shade.decima.ui.editor.core;

import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.navigator.NavigatorTreeCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.icons.OverlaidIcon;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;

public class CoreTreeCellRenderer extends NavigatorTreeCellRenderer {
    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof CoreNodeEntry entry) {
            // TODO: Add a preference for toggling this on/off
            append("[%d] ".formatted(entry.getIndex()), TextAttributes.GRAYED_ATTRIBUTES);
        }

        if (value instanceof CoreNodeObject node) {
            final ValueHandler.Decorator decorator = node.getHandler().getDecorator(node.getType());

            append(node.getLabel(), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
            append(" = ", TextAttributes.REGULAR_ATTRIBUTES);
            append("{%s}".formatted(node.getType().getFullTypeName()), TextAttributes.GRAYED_ATTRIBUTES);

            if (decorator != null) {
                if (decorator.needsGap()) {
                    append(" ", TextAttributes.REGULAR_ATTRIBUTES);
                }

                decorator.decorate(node.getValue(), this);
            }
        } else if (value instanceof CoreNodeBinary) {
            append(value.getLabel(), TextAttributes.GRAYED_ATTRIBUTES);
        } else if (value instanceof CoreNodeEntryGroup group) {
            append(group.getLabel(), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
        } else {
            super.customizeCellRenderer(tree, value, selected, expanded, focused, leaf, row);
        }
    }

    @Nullable
    @Override
    public Icon getIcon(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        final Icon icon = super.getIcon(tree, value, selected, expanded, focused, leaf, row);

        if (icon != null && value instanceof CoreNodeObject object) {
            return switch (object.getState()) {
                case NEW -> new OverlaidIcon(icon, UIManager.getIcon("Overlay.addIcon"));
                case CHANGED -> new OverlaidIcon(icon, UIManager.getIcon("Overlay.modifyIcon"));
                case UNCHANGED -> icon;
            };
        }

        return icon;
    }
}
