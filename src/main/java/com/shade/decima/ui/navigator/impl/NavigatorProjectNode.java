package com.shade.decima.ui.navigator.impl;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileChangeListener;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.packfile.edit.Change;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class NavigatorProjectNode extends NavigatorNode {
    private final ProjectContainer container;
    private Icon icon;
    private Project project;

    public NavigatorProjectNode(@Nullable NavigatorNode parent, @NotNull ProjectContainer container) {
        super(parent);
        this.container = container;
    }

    @NotNull
    public Project getProject() {
        return Objects.requireNonNull(project, "Node is not initialized");
    }

    @NotNull
    @Override
    public ProjectContainer getProjectContainer() {
        return container;
    }

    @NotNull
    @Override
    public String getLabel() {
        return container.getName();
    }

    @NotNull
    @Override
    protected NavigatorNode[] loadChildren(@NotNull ProgressMonitor monitor) throws IOException {
        project = new Project(container);
        project.mountDefaults();

        final PackfileManager manager = project.getPackfileManager();
        final List<NavigatorPackfileNode> children = new ArrayList<>();

        manager.addChangeListener(new PackfileChangeListener() {
            @Override
            public void changeAdded(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {
                updateNode(packfile, path);
            }

            @Override
            public void changeRemoved(@NotNull Packfile packfile, @NotNull FilePath path, @NotNull Change change) {
                updateNode(packfile, path);
            }

            private void updateNode(@NotNull Packfile packfile, @NotNull FilePath path) {
                final NavigatorTreeModel model = Application.getFrame().getNavigator().getModel();
                // NOTE: Node must ALWAYS be loaded and NO EXCEPTIONS must be thrown
                model
                    .findFileNode(new VoidProgressMonitor(), container, packfile, path.parts())
                    .whenComplete((node, exception) -> model.fireNodesChanged(node));
            }
        });

        for (Packfile packfile : manager.getPackfiles()) {
            if (packfile.isEmpty()) {
                continue;
            }

            children.add(new NavigatorPackfileNode(this, packfile));
        }

        children.sort(Comparator.comparing(NavigatorNode::getLabel));

        return children.toArray(NavigatorNode[]::new);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        if (icon == null) {
            icon = FileSystemView.getFileSystemView().getSystemIcon(container.getExecutablePath().toFile());
        }

        return icon;
    }

    public void resetIcon() {
        icon = null;
    }

    @Override
    public void clear() {
        super.clear();

        try {
            project.close();
            project = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
