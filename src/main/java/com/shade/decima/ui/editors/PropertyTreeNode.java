package com.shade.decima.ui.editors;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.handlers.ValueHandler;

import javax.swing.tree.DefaultMutableTreeNode;

public class PropertyTreeNode extends DefaultMutableTreeNode {
    private final RTTIType<?> type;
    private final ValueHandler handler;
    private final String name;

    public PropertyTreeNode(@NotNull RTTIType<?> type, @NotNull ValueHandler handler, @Nullable String name, @NotNull Object value) {
        this.type = type;
        this.handler = handler;
        this.name = name;
        this.userObject = value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("<html>");
        final String inline = handler.getInlineValue(type, userObject);

        if (name != null) {
            sb.append("<font color=#7f0000>%s</font> = ".formatted(UIUtils.escapeHtmlEntities(name)));
        }

        if (inline != null) {
            sb.append(inline);
        } else {
            sb.append("<font color=gray>{%s}</font>".formatted(UIUtils.escapeHtmlEntities(RTTITypeRegistry.getFullTypeName(type))));
        }

        if (type.getKind() == RTTIType.Kind.CONTAINER) {
            sb.append(" size = ").append(children.size());
        }

        return sb.toString();
    }

    @NotNull
    public RTTIType<?> getType() {
        return type;
    }
}
