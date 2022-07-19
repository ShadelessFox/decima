package com.shade.decima.ui.editor.property;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handler.ValueCollectionHandler;
import com.shade.decima.ui.handler.ValueHandler;
import com.shade.decima.ui.handler.ValueHandlerProvider;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.util.Collection;

public class PropertyObjectNode extends NavigatorLazyNode {
    private final RTTIType<?> type;
    private final ValueHandler handler;
    private final Object object;
    private final String name;

    public PropertyObjectNode(@Nullable NavigatorNode parent, @NotNull RTTIType<?> type, @NotNull Object object, @Nullable String name) {
        super(parent);
        this.type = type;
        this.handler = ValueHandlerProvider.getValueHandler(type);
        this.object = object;
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        if (handler instanceof ValueCollectionHandler<?, ?>) {
            final ValueCollectionHandler<Object, Object> collection = (ValueCollectionHandler<Object, Object>) handler;
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
        return handler instanceof ValueCollectionHandler;
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
