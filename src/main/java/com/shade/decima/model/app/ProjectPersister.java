package com.shade.decima.model.app;

import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.*;

public class ProjectPersister {
    private final Map<NavigatorFileNode, List<Change>> changes = new HashMap<>();

    public void addChange(@NotNull NavigatorFileNode node, @NotNull Change change) {
        changes
            .computeIfAbsent(node, key -> new ArrayList<>())
            .add(change);
    }

    public void clearChanges() {
        changes.clear();
    }

    public void clearChanges(@NotNull NavigatorFileNode node) {
        changes.remove(node);
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public boolean hasChangesInPath(@NotNull TreeNode node) {
        if (changes.isEmpty()) {
            return false;
        }

        for (NavigatorFileNode changedNode : changes.keySet()) {
            for (TreeNode currentNode = changedNode; currentNode != null; currentNode = currentNode.getParent()) {
                if (currentNode == node) {
                    return true;
                }
            }
        }

        return false;
    }

    @NotNull
    public List<Change> getChanges(@NotNull NavigatorFileNode node) {
        final List<Change> changes = this.changes.get(node);
        if (changes.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(changes);
        }
    }

    @NotNull
    public Change getMergedChange(@NotNull NavigatorFileNode node) {
        final List<Change> changes = getChanges(node);

        if (changes.isEmpty()) {
            throw new IllegalStateException("No changes");
        }

        Change merged = changes.get(0);

        for (int i = 1; i < changes.size(); i++) {
            merged = merged.merge(changes.get(i));
        }

        return merged;
    }

    @NotNull
    public Set<NavigatorFileNode> getFiles() {
        return Collections.unmodifiableSet(changes.keySet());
    }

    public interface Change {
        /**
         * Merges current, old change with other, new {@code change}.
         *
         * @param change other change to merge current change with
         * @return merged change
         */
        @NotNull
        Change merge(@NotNull Change change);

        @NotNull
        Resource toResource() throws IOException;
    }
}
