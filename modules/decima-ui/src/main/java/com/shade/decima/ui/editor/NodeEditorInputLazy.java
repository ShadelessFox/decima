package com.shade.decima.ui.editor;

import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.util.FilePath;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.decima.ui.navigator.impl.NavigatorPackfileNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.platform.model.SaveableElement;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.lazy.LazyEditorInput;
import com.shade.platform.ui.editors.lazy.UnloadableEditorInput;
import com.shade.util.NotNull;

import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public record NodeEditorInputLazy(@NotNull UUID container, @NotNull String packfile, @NotNull FilePath path, boolean canLoadImmediately) implements LazyEditorInput, UnloadableEditorInput, SaveableElement {
    public NodeEditorInputLazy(@NotNull UUID container, @NotNull String packfile, @NotNull FilePath path) {
        this(container, packfile, path, true);
    }

    public NodeEditorInputLazy(@NotNull String container, @NotNull String packfile, @NotNull String path) {
        this(UUID.fromString(container), packfile, new FilePath(path.split("/")));
    }

    public NodeEditorInputLazy(@NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String path) {
        this(container.getId(), packfile.getPath().getFileName().toString(), new FilePath(path.split("/")));
    }

    @NotNull
    public static NodeEditorInputLazy from(@NotNull NodeEditorInput input) {
        return new NodeEditorInputLazy(
            input.getProject().getContainer(),
            input.getNode().getPackfile(),
            input.getNode().getPath().full()
        );
    }

    @Override
    @NotNull
    public NodeEditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
        final NavigatorFileNode node;

        try {
            node = findFileNode(Application.getNavigator(), monitor).get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }

        if (node != null) {
            return new NodeEditorInputSimple(node);
        } else {
            throw new IllegalArgumentException("Unable to load real input");
        }
    }

    @NotNull
    @Override
    public LazyEditorInput canLoadImmediately(boolean canLoadImmediately) {
        return new NodeEditorInputLazy(container, packfile, path, canLoadImmediately);
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
        if (other instanceof NodeEditorInputSimple o) {
            return container().equals(o.getNode().getProjectContainer().getId())
                && packfile().equals(o.getNode().getPackfile().getPath().getFileName().toString())
                && path().equals(o.getNode().getPath());
        }
        return equals(other);
    }

    @NotNull
    @Override
    public LazyEditorInput unloadInput() {
        return canLoadImmediately(false);
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        state.put("project", container.toString());
        state.put("packfile", packfile);
        state.put("resource", path.full());
    }

    @NotNull
    @Override
    public String getFactoryId() {
        return NodeEditorInputFactory.ID;
    }

    @NotNull
    private CompletableFuture<NavigatorFileNode> findFileNode(@NotNull NavigatorTree navigator, @NotNull ProgressMonitor monitor) {
        CompletableFuture<? extends TreeNode> future;

        future = navigator.getModel().findChild(
            monitor,
            navigator.getModel().getRoot(),
            child -> child instanceof NavigatorProjectNode n && n.getProjectContainer().getId().equals(container),
            () -> "Can't find container node '" + container + "'"
        );

        future = future.thenCompose(node -> navigator.getModel().findChild(
            monitor,
            node,
            child -> child instanceof NavigatorPackfileNode n && n.getPackfile().getPath().getFileName().toString().equals(packfile),
            () -> "Can't find packfile node '" + packfile + "'"
        ));

        for (String part : path.parts()) {
            future = future.thenCompose(node -> navigator.getModel().findChild(
                monitor,
                node,
                child -> child.getLabel().equals(part),
                () -> "Can't find file node '" + path.full() + "'"
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }
}
