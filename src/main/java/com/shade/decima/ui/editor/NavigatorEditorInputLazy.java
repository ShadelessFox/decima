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
import com.shade.util.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record NavigatorEditorInputLazy(@NotNull UUID container, @NotNull String packfile, @NotNull FilePath path) implements LazyEditorInput {
    public NavigatorEditorInputLazy(@NotNull String container, @NotNull String packfile, @NotNull String path) {
        this(UUID.fromString(container), packfile, new FilePath(path.split("/")));
    }

    public NavigatorEditorInputLazy(@NotNull ProjectContainer container, @NotNull Packfile packfile, @NotNull String path) {
        this(container.getId(), packfile.getPath().getFileName().toString(), new FilePath(path.split("/")));
    }

    @NotNull
    public NavigatorEditorInput loadRealInput(@NotNull ProgressMonitor monitor) throws Exception {
        final NavigatorTree navigator = Application.getFrame().getNavigator();
        final NavigatorFileNode node = findFileNode(navigator, monitor).get();

        if (node != null) {
            return new NavigatorEditorInputImpl(node);
        } else {
            throw new IllegalArgumentException("Unable to load real input");
        }
    }

    @NotNull
    @Override
    public String getName() {
        return path.last();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean representsSameResource(@NotNull EditorInput other) {
        if (other instanceof NavigatorEditorInputImpl o) {
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
            child -> child instanceof NavigatorProjectNode n && n.getProjectContainer().getId().equals(container)
        );

        future = future.thenCompose(node -> navigator.getModel().findChild(
            monitor,
            node,
            child -> child instanceof NavigatorPackfileNode n && n.getPackfile().getPath().getFileName().toString().equals(packfile)
        ));

        for (String part : path.parts()) {
            future = future.thenCompose(node -> navigator.getModel().findChild(
                monitor,
                node,
                child -> child.getLabel().equals(part)
            ));
        }

        return future.thenApply(node -> (NavigatorFileNode) node);
    }
}
