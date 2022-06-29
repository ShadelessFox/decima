package com.shade.decima.ui.editor;

import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

public class PropertyRootNode extends NavigatorLazyNode {
    private final CoreBinary binary;

    public PropertyRootNode(@Nullable NavigatorNode parent, @NotNull CoreBinary binary) {
        super(parent);
        this.binary = binary;
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        return binary.entries().stream()
            .map(entry -> new PropertyObjectNode(
                this,
                entry.getType(),
                entry,
                RTTITypeRegistry.getFullTypeName(entry.getType())
            ))
            .toArray(NavigatorNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }
}
