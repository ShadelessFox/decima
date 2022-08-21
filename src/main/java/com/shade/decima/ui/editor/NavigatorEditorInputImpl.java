package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.StringJoiner;

public record NavigatorEditorInputImpl(@NotNull NavigatorFileNode node) implements NavigatorEditorInput {
    @NotNull
    @Override
    public String getName() {
        return node.getLabel();
    }

    @Nullable
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + node.getProject().getContainer().getName());
        joiner.add("Packfile: " + node.getPackfile().getName());
        joiner.add("Path: " + node.getPath().full());
        return joiner.toString();
    }

    @NotNull
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
        return node.getProject();
    }
}
