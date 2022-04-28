package com.shade.decima.ui.editor;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.handler.ValueHandler;

import javax.swing.tree.DefaultMutableTreeNode;

public class PropertyTreeNode extends DefaultMutableTreeNode {
    private final RTTIType<?> type;
    private final ValueHandler handler;
    private final String name;
    private State state;

    public PropertyTreeNode(@NotNull RTTIType<?> type, @NotNull ValueHandler handler, @Nullable String name, @NotNull Object value) {
        this.type = type;
        this.handler = handler;
        this.name = name;
        this.userObject = value;
        this.state = State.EXISTING;
    }

    @NotNull
    public State getState() {
        return state;
    }

    public void setState(@NotNull State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("<html>");
        final String inline = handler.getInlineValue(type, userObject);

        if (name != null) {
            sb.append("<font color=#7f0000>%s</font> = ".formatted(UIUtils.escapeHtmlEntities(name)));
        }

        sb.append("<font color=gray>{%s}</font>".formatted(UIUtils.escapeHtmlEntities(RTTITypeRegistry.getFullTypeName(type))));

        if (inline != null) {
            sb.append(' ').append(inline);
        }

        if (type.getKind() == RTTIType.Kind.CONTAINER) {
            sb.append(" size = ").append(children == null ? 0 : children.size());
        }

        return sb.toString();
    }

    @NotNull
    public RTTIType<?> getType() {
        return type;
    }

    public enum State {
        EXISTING,
        MODIFIED,
        CREATED
    }
}
