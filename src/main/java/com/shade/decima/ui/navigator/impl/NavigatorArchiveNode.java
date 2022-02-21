package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.archive.ArchiveManager;
import com.shade.decima.model.rtti.objects.RTTICollection;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.NavigatorLazyNode;
import com.shade.decima.ui.navigator.NavigatorNode;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NavigatorArchiveNode extends NavigatorLazyNode {
    private final NavigatorProjectNode parent;
    private final Archive archive;

    public NavigatorArchiveNode(@NotNull NavigatorProjectNode parent, @NotNull Archive archive) {
        this.parent = parent;
        this.archive = archive;
    }

    @NotNull
    @Override
    protected List<? extends NavigatorNode> loadChildren(@NotNull PropertyChangeListener listener) throws Exception {
        final ArchiveManager manager = parent.getProject().getArchiveManager();
        final RTTIObject prefetch = manager.readFileObjects(parent.getProject().getCompressor(), "prefetch/fullgame.prefetch").get(0);
        final LookupNode root = new LookupNode("root", null);

        for (RTTIObject file : prefetch.<RTTICollection<RTTIObject>>get("Files")) {
            final String path = file.get("Path");
            final Archive.FileEntry entry = manager.getFileEntry(path);

            if (entry != null && entry.archive() == archive) {
                root.populate(path, entry);
            }
        }

        return root.toTreeNodeList(this);
    }

    @Nullable
    @Override
    public String getLabel() {
        return archive.getName();
    }

    @Nullable
    @Override
    public NavigatorProjectNode getParent() {
        return parent;
    }

    // TODO: This node is used for caching purposes and fast child lookup by its name.
    //       Can we replace it with something more generic?
    private static class LookupNode {
        private final String name;
        private final Map<String, LookupNode> children;
        private final Archive.FileEntry entry;

        public LookupNode(@NotNull String name, @Nullable Archive.FileEntry entry) {
            this.name = name;
            this.entry = entry;
            this.children = new TreeMap<>();
        }

        public void populate(@NotNull String path, @Nullable Archive.FileEntry entry) {
            LookupNode root = this;

            final String[] parts = path.split("/");

            for (int i = 0; i < parts.length; i++) {
                final String part = parts[i];
                final boolean leaf = i == parts.length - 1;

                LookupNode child = root.children.get(part);

                if (child == null) {
                    child = new LookupNode(part, leaf ? entry : null);
                    root.children.put(part, child);
                }

                root = child;
            }
        }

        @NotNull
        public List<NavigatorNode> toTreeNodeList(@NotNull NavigatorNode parent) {
            final List<NavigatorNode> nodes = new ArrayList<>();
            for (LookupNode child : children.values()) {
                nodes.add(child.toTreeNode(parent));
            }
            return nodes;
        }

        @NotNull
        public NavigatorNode toTreeNode(@NotNull NavigatorNode parent) {
            if (entry != null) {
                return new NavigatorFileNode(parent, name, entry);
            }

            final List<NavigatorNode> nodes = new ArrayList<>();
            final NavigatorFolderNode node = new NavigatorFolderNode(parent, nodes, name);

            nodes.addAll(toTreeNodeList(node));

            return node;

        }
    }
}
