package com.shade.decima.ui.editor;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.FilePath;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.util.NotNull;

import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public record FileEditorInputLazy(@NotNull UUID container, @NotNull String packfile, @NotNull FilePath path, boolean canLoadImmediately) implements LazyEditorInput {
    public FileEditorInputLazy(@NotNull UUID container, @NotNull String packfile, @NotNull FilePath path) {
        this(container, packfile, path, true);
    }

    public FileEditorInputLazy(@NotNull String container, @NotNull String packfile, @NotNull String path) {
        this(UUID.fromString(container), packfile, new FilePath(path.split("/")));
    }

    public FileEditorInputLazy(@NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String path) {
        this(container.getId(), packfile.getPath().getFileName().toString(), new FilePath(path.split("/")));
    }

    @NotNull
    public static FileEditorInputLazy from(@NotNull FileEditorInput input) {
        return new FileEditorInputLazy(
            input.getProject().getContainer(),
            input.getNode().getPackfile(),
            input.getNode().getPath().full()
        );
    }

    @NotNull
    public FileEditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
        final NavigatorTree navigator = Application.getFrame().getNavigator();
        final NavigatorFileNode node;

        try {
            node = findFileNode(navigator, monitor).get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }

        if (node != null) {
            return new FileEditorInputSimple(node);
        } else {
            throw new IllegalArgumentException("Unable to load real input");
        }
    }

    @NotNull
    @Override
    public LazyEditorInput canLoadImmediately(boolean canLoadImmediately) {
        return new FileEditorInputLazy(container, packfile, path, canLoadImmediately);
    }

    @NotNull
    @Override
    public String getName() {
        return path.last();
    }

    @NotNull
    @Override
    public String getDescription() {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Project: " + container);
        joiner.add("Packfile: " + packfile);
        joiner.add("Path: " + path.full());
        return joiner.toString();
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        if (other instanceof FileEditorInputSimple o) {
            return container().equals(o.getNode().getProjectContainer().getId())
                && packfile().equals(o.getNode().getPackfile().getPath().getFileName().toString())
                && path().equals(o.getNode().getPath());
        }
        return equals(other);
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findFileNode(@NotNull NavigatorTree navigator, @NotNull ProgressMonitor monitor) {
        CompletableFuture<? extends TreeNode> future;

        future = navigator.getModel().findChild(
            monitor,
            navigator.getModel().getRoot(),
            (child, index) -> child instanceof NavigatorProjectNode n && n.getProjectContainer().getId().equals(container)
        );

        future = future.thenCompose(node -> navigator.getModel().findChild(
            monitor,
            node,
            (child, index) -> child instanceof NavigatorPackfileNode n && n.getPackfile().getPath().getFileName().toString().equals(packfile)
        ));

        for (String part : path.parts()) {
            future = future.thenCompose(node -> navigator.getModel().findChild(
                monitor,
                node,
                (child, index) -> child.getLabel().equals(part)
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }
}
