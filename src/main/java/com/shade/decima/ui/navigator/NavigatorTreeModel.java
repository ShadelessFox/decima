package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorFolderNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.prefs.Preferences;

public class NavigatorTreeModel extends DefaultTreeModel {
    public final boolean groupByArchive;
    public final boolean groupByStructure;

    public NavigatorTreeModel(@NotNull Workspace workspace, @Nullable TreeNode root) {
        super(root);

        final Preferences node = workspace.getPreferences().node("navigator");
        this.groupByArchive = node.getBoolean("group_by_archive", true);
        this.groupByStructure = node.getBoolean("group_by_structure", true);
    }

    @Nullable
    public Object getClassifierKey(@Nullable NavigatorNode parent, @NotNull NavigatorNode node) {
        if (groupByArchive && parent instanceof NavigatorProjectNode && node instanceof NavigatorFileNode file) {
            return file.getFile().archive();
        }
        if (groupByStructure && node instanceof NavigatorFileNode file) {
            final int depth = getDepth(parent);
            final String[] path = file.getPath();
            if (path.length - 1 > depth) {
                return path[depth];
            } else {
                file.setDepth(depth);
                return null;
            }
        }
        return null;
    }

    @NotNull
    public String getClassifierLabel(@Nullable NavigatorNode parent, @NotNull Object key) {
        if (key instanceof Archive archive) {
            return archive.getName();
        }
        if (key instanceof String str) {
            return str;
        }
        throw new IllegalArgumentException("parent=" + parent + ", key=" + key);
    }

    @NotNull
    public NavigatorNode getClassifierNode(@Nullable NavigatorNode parent, @NotNull Object key, @NotNull List<? extends NavigatorNode> children) {
        if (groupByArchive && parent instanceof NavigatorProjectNode) {
            return new NavigatorFolderNode(parent, children, getClassifierLabel(parent, key), -1);
        }
        return new NavigatorFolderNode(parent, children, getClassifierLabel(parent, key), getDepth(parent) + 1);
    }

    private static int getDepth(@Nullable NavigatorNode node) {
        if (node instanceof NavigatorFolderNode folder) {
            final int depth = folder.getDepth();
            if (depth > 0) {
                return depth;
            }
        }
        return 0;
    }
}
