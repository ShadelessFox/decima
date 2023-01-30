package com.shade.decima.ui.editor.core;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class CoreNodeEntryGroup extends TreeNodeLazy {
    private final RTTIType<?> type;

    public CoreNodeEntryGroup(@Nullable CoreNodeBinary parent, @NotNull RTTIType<?> type) {
        super(parent);
        this.type = type;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) throws Exception {
        final CoreNodeBinary parent = getParentOfType(CoreNodeBinary.class);

        Stream<RTTIObject> stream = parent.getBinary().entries().stream()
            .filter(entry -> entry.type() == type);

        if (parent.isSortingEnabled()) {
            stream = stream.sorted(Comparator.comparing(entry -> entry.type().getTypeName()));
        }

        return stream
            .collect(Collector.of(
                ArrayList<TreeNode>::new,
                (left, entry) -> left.add(new CoreNodeEntry(this, entry, left.size())),
                (left, right) -> { left.addAll(right); return left; }
            ))
            .toArray(TreeNode[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return type.getTypeName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("Node.archiveIcon");
    }

    public boolean contains(@NotNull RTTIPathElement.UUID element) {
        final CoreBinary binary = getParentOfType(CoreNodeBinary.class).getBinary();

        for (RTTIObject entry : binary.entries()) {
            if (entry.type() == type && element.equals(new RTTIPathElement.UUID(entry))) {
                return true;
            }
        }

        return false;
    }
}
