package com.shade.decima.ui.navigator;

import com.shade.decima.Project;
import com.shade.decima.archive.Archive;
import com.shade.decima.archive.ArchiveManager;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NavigatorProjectNode extends NavigatorNode {
    private final Project project;
    private final List<NavigatorNode> children;

    public NavigatorProjectNode(@NotNull Project project) throws IOException {
        this.project = project;
        this.children = new ArrayList<>();

        final RTTIObject prefetch = project.getArchiveManager().readFileObjects(project.getCompressor(), "prefetch/fullgame.prefetch").get(0);
        final LookupNode root = new LookupNode("root", null);

        for (RTTIObject file : (RTTIObject[]) prefetch.getMemberValue("Files")) {
            final String path = file.getMemberValue("Path");
            final Archive.FileEntry entry = project.getArchiveManager().getFileEntry(path);

            if (entry != null) {
                root.populate(entry.archive().getName() + '/' + path, entry);
            }
        }

        this.children.addAll(root.toTreeNode(null).getChildren());
    }

    @Nullable
    @Override
    public String getLabel() {
        return project.getExecutablePath().getFileName().toString();
    }

    @NotNull
    @Override
    public List<NavigatorNode> getChildren() {
        return children;
    }

    @Nullable
    @Override
    public NavigatorNode getParent() {
        return null;
    }

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

            for (String part : path.split("/")) {
                LookupNode child = root.children.get(part);

                if (child == null) {
                    child = new LookupNode(part, entry);
                    root.children.put(part, child);
                }

                root = child;
            }
        }

        @NotNull
        public NavigatorNode toTreeNode(@Nullable NavigatorNode parent) {
            if (!children.isEmpty()) {
                final List<NavigatorNode> nodes = new ArrayList<>();
                final NavigatorFolderNode node = new NavigatorFolderNode(parent, nodes, name);

                for (LookupNode child : children.values()) {
                    nodes.add(child.toTreeNode(node));
                }

                return node;
            }

            return new NavigatorFileNode(parent, name, entry);
        }
    }
}
