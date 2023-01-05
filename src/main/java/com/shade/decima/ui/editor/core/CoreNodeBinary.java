package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

import java.util.List;
import java.util.stream.IntStream;

public class CoreNodeBinary extends TreeNodeLazy {
    private final CoreBinary binary;
    private final ProjectContainer project;
    private boolean groupingEnabled;

    public CoreNodeBinary(@NotNull CoreBinary binary, @NotNull ProjectContainer project) {
        super(null);
        this.binary = binary;
        this.project = project;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        final List<RTTIObject> entries = binary.entries();

        if (groupingEnabled) {
            return entries.stream()
                .map(RTTIObject::type)
                .distinct()
                .map(type -> new CoreNodeEntryGroup(this, type))
                .toArray(TreeNode[]::new);
        } else {
            return IntStream.range(0, entries.size())
                .mapToObj(i -> new CoreNodeEntry(this, entries.get(i), i))
                .toArray(TreeNode[]::new);
        }
    }

    @NotNull
    @Override
    public String getLabel() {
        return "<root>";
    }

    @NotNull
    public CoreBinary getBinary() {
        return binary;
    }

    @NotNull
    public GameType getGameType() {
        return project.getType();
    }

    public boolean isGroupingEnabled() {
        return groupingEnabled;
    }

    public void setGroupingEnabled(boolean groupingEnabled) {
        this.groupingEnabled = groupingEnabled;
    }
}
