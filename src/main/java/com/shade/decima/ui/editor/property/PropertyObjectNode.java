package com.shade.decima.ui.editor.property;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.ValueHandlerProvider;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.util.Collection;

public class PropertyObjectNode extends TreeNodeLazy {
    private final RTTIType<?> type;
    private final ValueHandler handler;
    private final Object object;
    private final String name;

    public PropertyObjectNode(@Nullable TreeNode parent, @NotNull RTTIType<?> type, @NotNull Object object, @Nullable String name) {
        super(parent);
        this.type = type;
        this.handler = ValueHandlerProvider.getValueHandler(type);
        this.object = object;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        if (handler instanceof ValueHandlerCollection<?, ?>) {
            final ValueHandlerCollection<Object, Object> collection = (ValueHandlerCollection<Object, Object>) handler;
            final Collection<?> children = collection.getChildren(type, object);

            return children.stream()
                .map(child -> new PropertyObjectNode(
                    this,
                    collection.getChildType(type, object, child),
                    collection.getChildValue(type, object, child),
                    collection.getChildName(type, object, child)
                ))
                .toArray(PropertyObjectNode[]::new);
        }

        return EMPTY_CHILDREN;
    }

    @Override
    protected boolean allowsChildren() {
        return handler instanceof ValueHandlerCollection;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Object";
    }

    @Nullable
    public String getName() {
        return name;
    }

    @NotNull
    public ValueHandler getHandler() {
        return handler;
    }

    @NotNull
    public RTTIType<?> getType() {
        return type;
    }

    @NotNull
    public Object getObject() {
        return object;
    }
}
