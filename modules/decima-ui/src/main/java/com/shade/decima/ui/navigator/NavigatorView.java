package com.shade.decima.ui.navigator;

import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.ProjectManager;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.dnd.NodeTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectsNode;
import com.shade.decima.ui.views.BaseView;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.views.ViewRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;

@ViewRegistration(id = NavigatorView.ID, label = "Projects", icon = "Node.archiveIcon", keystroke = "alt 1")
public class NavigatorView extends BaseView<NavigatorTree> {
    public static final String ID = "navigator";

    @NotNull
    @Override
    public JComponent createComponent() {
        final JScrollPane pane = new JScrollPane(super.createComponent());
        pane.setBorder(null);
        pane.setPreferredSize(new Dimension(250, 0));

        return pane;
    }

    @NotNull
    @Override
    protected NavigatorTree createComponentImpl() {
        final NavigatorTree tree = new NavigatorTree(new NavigatorProjectsNode());
        tree.setRootVisible(false);
        tree.setTransferHandler(new NodeTransferHandler());
        tree.setDropTarget(null);
        tree.setDragEnabled(true);

        MenuManager.getInstance().installContextMenu(tree, MenuConstants.CTX_MENU_NAVIGATOR_ID, key -> switch (key) {
            case "selection" -> tree.getLastSelectedPathComponent();
            case "project" -> {
                if (tree.getLastSelectedPathComponent() instanceof NavigatorNode node) {
                    final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);

                    if (parent != null && parent.isOpen()) {
                        yield parent.getProject();
                    }
                }

                yield null;
            }
            case "projectContainer" -> {
                if (tree.getLastSelectedPathComponent() instanceof NavigatorNode node) {
                    final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);

                    if (parent != null) {
                        yield parent.getProjectContainer();
                    }
                }

                yield null;
            }
            case "editorManager" -> EditorManager.getInstance();
            default -> null;
        });

        MessageBus.getInstance().connect().subscribe(ProjectManager.PROJECTS, new ProjectChangeListener() {
            @Override
            public void projectAdded(@NotNull ProjectContainer container) {
                final var model = tree.getModel();
                final var workspaceNode = (NavigatorProjectsNode) model.getRoot();
                final var projectNode = new NavigatorProjectNode(workspaceNode, container);
                final int childIndex = IOUtils.indexOf(ProjectManager.getInstance().getProjects(), container);

                workspaceNode.addChild(projectNode, childIndex);
                model.fireStructureChanged(workspaceNode);
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
                final var workspaceNode = (NavigatorProjectsNode) model.getRoot();
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
