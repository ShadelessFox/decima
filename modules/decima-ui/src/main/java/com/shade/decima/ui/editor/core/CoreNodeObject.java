package com.shade.decima.ui.editor.core;

import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.model.rtti.types.RTTITypeArray;
import com.shade.decima.ui.data.ValueController.EditType;
import com.shade.decima.ui.data.ValueHandler;
import com.shade.decima.ui.data.ValueHandlerCollection;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Objects;

public class CoreNodeObject extends TreeNodeLazy {
    private final CoreEditor editor;
    private final RTTIType<?> type;
    private final String name;
    private final RTTIPath path;
    private ValueHandler handler;
    private State state;

    public CoreNodeObject(@NotNull TreeNode parent, @NotNull CoreEditor editor, @NotNull RTTIType<?> type, @NotNull String name, @NotNull RTTIPath path) {
        super(parent);
        this.editor = editor;
        this.type = type;
        this.name = name;
        this.path = path;
        this.state = State.UNCHANGED;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        if (allowsChildren()) {
            final var value = getValue();
            final var handler = (ValueHandlerCollection<Object, RTTIPathElement>) getHandler();
            final var elements = handler.getElements(type, value);
            final var children = new CoreNodeObject[elements.length];

            for (int i = 0; i < children.length; i++) {
                final RTTIPathElement element = elements[i];

                children[i] = new CoreNodeObject(
                    this,
                    editor,
                    handler.getElementType(type, value, element),
                    handler.getElementName(type, value, element),
                    path.concat(element)
                );
            }

            return children;
        }

        return EMPTY_CHILDREN;
    }

    @Override
    protected boolean allowsChildren() {
        return getHandler() instanceof ValueHandlerCollection;
    }

    @Override
    public boolean loadChildrenInBackground() {
        return !(type instanceof RTTITypeArray);
    }

    @NotNull
    @Override
    public String getLabel() {
        return name;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return getHandler().getIcon(type);
    }

    @NotNull
    public ValueHandler getHandler() {
        if (handler == null) {
            synchronized (this) {
                if (handler == null) {
                    handler = ValueRegistry.getInstance().findHandler(new CoreValueController<>(editor, this, EditType.INLINE));
                }
            }
        }

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
        return path.get(editor.getBinary());
    }

    public void setValue(@NotNull Object value) {
        path.set(editor.getBinary(), value);
    }

    @NotNull
    public State getState() {
        return state;
    }

    public void setState(@NotNull State state) {
        this.state = state;
    }

    @NotNull
    public RTTIPath getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoreNodeObject that = (CoreNodeObject) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public enum State {
        UNCHANGED,
        CHANGED,
        NEW
    }
}
