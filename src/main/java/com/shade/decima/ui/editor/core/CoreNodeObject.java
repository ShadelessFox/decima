package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.Path;
import com.shade.decima.model.rtti.path.PathElement;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Collection;

public class CoreNodeObject extends TreeNodeLazy {
    private final RTTIType<?> type;
    private final String name;
    private final PathElement element;
    private final Path path;
    private ValueHandler handler;

    public CoreNodeObject(@NotNull TreeNode parent, @NotNull RTTIType<?> type, @NotNull String name, @NotNull PathElement element) {
        super(parent);
        this.type = type;
        this.name = name;
        this.element = element;
        this.path = new Path(getPathToRoot(this, 0));
        this.handler = ValueRegistry.getInstance().findHandler(getValue(), getParentOfType(CoreNodeBinary.class).getGameType());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        if (handler instanceof ValueHandlerCollection<?, ?>) {
            final ValueHandlerCollection<Object, Object> collection = (ValueHandlerCollection<Object, Object>) handler;
            final Object object = getValue();
            final Collection<?> children = collection.getChildren(type, object);

            return children.stream()
                .map(child -> new CoreNodeObject(
                    this,
                    collection.getChildType(type, object, child),
                    collection.getChildName(type, object, child),
                    collection.getChildElement(type, object, child)
                ))
                .toArray(CoreNodeObject[]::new);
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
        return name;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return handler.getIcon(type);
    }

    @NotNull
    public ValueHandler getHandler() {
        return handler;
    }

    public void setHandler(@NotNull ValueHandler handler) {
        this.handler = handler;
    }

    @NotNull
    public RTTIType<?> getType() {
        return type;
    }

    @NotNull
    public Object getValue() {
        return path.get(getParentOfType(CoreNodeBinary.class).getBinary());
    }

    public void setValue(@NotNull Object value) {
        path.set(getParentOfType(CoreNodeBinary.class).getBinary(), value);
    }

    @NotNull
    public Path getPath() {
        return path;
    }

    @NotNull
    protected static PathElement[] getPathToRoot(@NotNull TreeNode node, int depth) {
        final TreeNode parent = node.getParent();
        final PathElement[] elements;

        if (parent instanceof CoreNodeBinary || parent == null) {
            elements = new PathElement[depth + 1];
        } else {
            elements = getPathToRoot(parent, depth + 1);
        }

        elements[elements.length - depth - 1] = ((CoreNodeObject) node).element;

        return elements;
    }
}
