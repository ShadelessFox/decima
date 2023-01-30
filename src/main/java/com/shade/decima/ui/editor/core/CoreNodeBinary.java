package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class CoreNodeBinary extends TreeNodeLazy {
    private final CoreBinary binary;
    private final ProjectContainer project;
    private boolean groupingEnabled;
    private boolean sortingEnabled;

    public CoreNodeBinary(@NotNull CoreBinary binary, @NotNull ProjectContainer project) {
        super(null);
        this.binary = binary;
        this.project = project;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        final List<RTTIObject> entries = binary.entries();

        Stream<RTTIObject> stream = entries.stream();

        if (sortingEnabled) {
            stream = stream.sorted(Comparator.comparing(entry -> entry.type().getTypeName()));
        }

        if (groupingEnabled) {
            return stream
                .map(RTTIObject::type).distinct()
                .map(type -> new CoreNodeEntryGroup(this, type))
                .toArray(TreeNode[]::new);
        } else {
            return stream
                .collect(Collector.of(
                    ArrayList<TreeNode>::new,
                    (left, entry) -> left.add(new CoreNodeEntry(this, entry, left.size())),
                    (left, right) -> { left.addAll(right); return left; }
                ))
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

    public boolean isSortingEnabled() {
        return sortingEnabled;
    }

    public void setSortingEnabled(boolean sortingEnabled) {
        this.sortingEnabled = sortingEnabled;
    }
}
