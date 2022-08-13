package com.shade.decima.ui.editor.lazy;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.editor.EditorInput;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;

import javax.swing.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LazyEditorInput implements EditorInput {
    private final UUID container;
    private final String packfile;
    private final String[] path;

    public LazyEditorInput(@NotNull UUID container, @NotNull String packfile, @NotNull String[] path) {
        this.container = container;
        this.packfile = packfile;
        this.path = path;
    }

    public LazyEditorInput(@NotNull String container, @NotNull String packfile, @NotNull String path) {
        this(UUID.fromString(container), packfile, path.split("/"));
    }

    public LazyEditorInput(@NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String path) {
        this(container.getId(), packfile.getPath().getFileName().toString(), path.split("/"));
    }

    @NotNull
    public EditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
        final NavigatorTree navigator = Application.getFrame().getNavigator();
        final NavigatorFileNode node = findFileNode(navigator, monitor).get();

        if (node != null) {
            return new NodeEditorInput(node);
        } else {
            throw new IllegalArgumentException("Unable to load real input");
        }
    }

    @NotNull
    @Override
    public String getName() {
        return path[path.length - 1];
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public NavigatorFileNode getNode() {
        throw new IllegalStateException("Node is not initialized");
    }

    @NotNull
    @Override
    public Project getProject() {
        throw new IllegalStateException("Node is not initialized");
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findFileNode(@NotNull NavigatorTree navigator, @NotNull ProgressMonitor monitor) {
        CompletableFuture<NavigatorNode> future;

        future = navigator.findChild(
            monitor,
            navigator.getModel().getRoot(),
            child -> child instanceof NavigatorProjectNode n && n.getContainer().getId().equals(container)
        );

        future = future.thenCompose(node -> navigator.findChild(
            monitor,
            node,
            child -> child instanceof NavigatorPackfileNode n && n.getPackfile().getPath().getFileName().toString().equals(packfile)
        ));

        for (String part : path) {
            future = future.thenCompose(node -> navigator.findChild(
                monitor,
                node,
                child -> child.getLabel().equals(part)
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }
}
