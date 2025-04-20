package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.AlphanumericComparator;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.controls.tree.TreeNodeLazy;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class CoreNodeFile extends TreeNodeLazy {
    private static final Comparator<CoreNodeEntryGroup> ENTRY_GROUP_COMPARATOR = Comparator
        .comparing(CoreNodeEntryGroup::getLabel, AlphanumericComparator.getInstance());

    private static final Comparator<CoreNodeEntry> ENTRY_COMPARATOR = Comparator
        .comparing(CoreNodeEntry::getLabel, AlphanumericComparator.getInstance())
        .thenComparing(CoreNodeEntry::getText, Comparator.nullsFirst(AlphanumericComparator.getInstance()));

    private final CoreEditor editor;
    private boolean groupingEnabled;
    private boolean sortingEnabled;

    public CoreNodeFile(@NotNull CoreEditor editor) {
        super(null);
        this.editor = editor;
    }

    @NotNull
    @Override
    protected TreeNode[] loadChildren(@NotNull ProgressMonitor monitor) {
        if (groupingEnabled) {
            return getGroupedEntries(this);
        } else {
            return getEntries(this, null);
        }
    }

    @NotNull
    public CoreNodeEntryGroup[] getGroupedEntries(@NotNull CoreNodeFile parent) {
        Stream<CoreNodeEntryGroup> stream = editor.getCoreFile().objects().stream()
            .map(RTTIObject::type)
            .distinct()
            .map(type -> new CoreNodeEntryGroup(parent, type));

        if (sortingEnabled) {
            stream = stream.sorted(ENTRY_GROUP_COMPARATOR);
        }

        return stream.toArray(CoreNodeEntryGroup[]::new);
    }

    @NotNull
    public CoreNodeEntry[] getEntries(@NotNull TreeNode parent, @Nullable RTTIType<?> type) {
        Stream<CoreNodeEntry> stream = getCoreFile().objects().stream()
            .filter(object -> type == null || object.type() == type)
            .map(object -> new CoreNodeEntry(parent, editor, object));

        if (sortingEnabled) {
            stream = stream.sorted(ENTRY_COMPARATOR);
        }

        return stream
            .collect(Collector.of(
                ArrayList<CoreNodeEntry>::new,
                (left, entry) -> {
                    left.add(entry);
                    entry.setIndex(left.size());
                },
                (left, right) -> {
                    left.addAll(right);
                    return left;
                }
            ))
            .toArray(CoreNodeEntry[]::new);
    }

    @NotNull
    @Override
    public String getLabel() {
        return editor.getInput().getName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return UIManager.getIcon("File.coreIcon");
    }

    @NotNull
    public CoreEditor getEditor() {
        return editor;
    }

    @NotNull
    public RTTICoreFile getCoreFile() {
        return editor.getCoreFile();
    }

    @NotNull
    public ProjectContainer getProject() {
        return editor.getInput().getProject().getContainer();
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
