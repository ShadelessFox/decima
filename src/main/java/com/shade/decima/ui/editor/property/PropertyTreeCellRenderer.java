package com.shade.decima.ui.editor.property;

import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.navigator.NavigatorTreeCellRenderer;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import javax.swing.*;

public class PropertyTreeCellRenderer extends NavigatorTreeCellRenderer {
    public PropertyTreeCellRenderer(@NotNull TreeModel model) {
        super(model);
    }

    @Override
    protected void customizeCellRenderer(@NotNull JTree tree, @NotNull TreeNode value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
        if (value instanceof PropertyObjectNode node) {
            final String name = node.getName();
            final ValueHandler handler = node.getHandler();

            if (name != null) {
                append(name, TextAttributes.DARK_RED_ATTRIBUTES);
                append(" = ", TextAttributes.REGULAR_ATTRIBUTES);
            }

            append("{%s}".formatted(RTTITypeRegistry.getFullTypeName(node.getType())), TextAttributes.GRAYED_ATTRIBUTES);

            if (handler.hasInlineValue()) {
                append(" ", TextAttributes.REGULAR_ATTRIBUTES);
                handler.appendInlineValue(node.getType(), node.getObject(), this);
            }
        } else if (value instanceof PropertyRootNode) {
            append(value.getLabel(), TextAttributes.GRAYED_ATTRIBUTES);
        } else {
            super.customizeCellRenderer(tree, value, selected, expanded, focused, leaf, row);
        }
    }
}
