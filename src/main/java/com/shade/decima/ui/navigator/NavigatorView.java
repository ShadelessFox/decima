package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.dnd.FileTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import com.shade.decima.ui.views.BaseView;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.views.ViewRegistration;
import com.shade.util.NotNull;

import javax.swing.*;

@ViewRegistration(id = "navigator", label = "Projects", icon = "Node.archiveIcon")
public class NavigatorView extends BaseView<NavigatorTree> {
    @NotNull
    @Override
    public JComponent createComponent() {
        final JScrollPane pane = new JScrollPane(super.createComponent());
        pane.setBorder(null);

        return pane;
    }

    @NotNull
    @Override
    protected NavigatorTree createComponentImpl() {
        final Workspace workspace = Application.getWorkspace();

        final NavigatorTree tree = new NavigatorTree(new NavigatorWorkspaceNode(workspace));
        tree.setRootVisible(false);
        tree.setTransferHandler(new FileTransferHandler());
        tree.setDropTarget(null);
        tree.setDragEnabled(true);

        Application.getMenuService().installPopupMenu(tree, MenuConstants.CTX_MENU_NAVIGATOR_ID, key -> switch (key) {
            case "workspace" -> workspace;
            case "selection" -> tree.getLastSelectedPathComponent();
            case "project" -> {
                final NavigatorNode node = (NavigatorNode) tree.getLastSelectedPathComponent();
                final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
                yield parent != null && !parent.needsInitialization() ? parent.getProject() : null;
            }
            case "projectContainer" -> {
                final NavigatorNode node = (NavigatorNode) tree.getLastSelectedPathComponent();
                final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
                yield parent != null ? parent.getProjectContainer() : null;
            }
            case "editorManager" -> Application.getEditorManager();
            default -> null;
        });

        workspace.addProjectChangeListener(new ProjectChangeListener() {
            @Override
            public void projectAdded(@NotNull ProjectContainer container) {
                final var model = tree.getModel();
                final var workspaceNode = (NavigatorWorkspaceNode) model.getRoot();
                final var projectNode = new NavigatorProjectNode(workspaceNode, container);
                final int childIndex = workspace.getProjects().indexOf(container);

                workspaceNode.addChild(projectNode, childIndex);
                model.fireNodesInserted(workspaceNode, childIndex);
            }

            @Override
            public void projectUpdated(@NotNull ProjectContainer container) {
                final var model = tree.getModel();
                final var projectNode = model.getProjectNode(new VoidProgressMonitor(), container);

                projectNode.resetIcon();
                model.fireNodesChanged(projectNode);
            }

            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                final var model = tree.getModel();
                final var workspaceNode = (NavigatorWorkspaceNode) model.getRoot();
                final var projectNode = model.getProjectNode(new VoidProgressMonitor(), container);
                final int childIndex = model.getIndexOfChild(workspaceNode, projectNode);

                workspaceNode.removeChild(childIndex);
                model.fireNodesRemoved(workspaceNode, childIndex);
            }

            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                final var model = tree.getModel();
                final var projectNode = model.getProjectNode(new VoidProgressMonitor(), container);

                model.unloadNode(projectNode);
            }
        });

        return tree;
    }

    @NotNull
    public NavigatorTree getTree() {
        return component;
    }
}
