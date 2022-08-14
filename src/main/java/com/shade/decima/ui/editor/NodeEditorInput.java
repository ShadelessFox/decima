package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import java.util.Objects;
import java.util.StringJoiner;

public class NodeEditorInput implements EditorInput {
    private final NavigatorFileNode node;

    public NodeEditorInput(@NotNull NavigatorFileNode node) {
        this.node = node;
    }

    @NotNull
    @Override
    public String getName() {
        return node.getLabel();
    }

    @Nullable
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + UIUtils.getProject(node).getContainer().getName());
        joiner.add("Packfile: " + UIUtils.getPackfile(node).getName());
        joiner.add("Path: " + node.getPath().full());
        return joiner.toString();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return Objects.requireNonNullElseGet(node.getIcon(), () -> UIManager.getIcon("Tree.leafIcon"));
    }

    @NotNull
    @Override
    public NavigatorFileNode getNode() {
        return node;
    }

    @NotNull
    @Override
    public Project getProject() {
        return UIUtils.getProject(node);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeEditorInput that = (NodeEditorInput) o;
        return node.equals(that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }
}
