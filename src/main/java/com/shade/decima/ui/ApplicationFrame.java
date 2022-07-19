package com.shade.decima.ui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.action.Actions;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorChangeListener;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.EditorStack;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.NavigatorTreeModel;
import com.shade.decima.ui.navigator.dnd.FileTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final NavigatorTree navigator;
    private final EditorStack editors;

    public ApplicationFrame() {
        try {
            this.workspace = new Workspace();
            this.navigator = new NavigatorTree(new NavigatorWorkspaceNode(workspace));
            this.editors = new EditorStack(workspace);

            setTitle(getApplicationTitle());
            setPreferredSize(new Dimension(1280, 720));

            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() {
        initializeMenuBar();
        initializeNavigatorPane();
        initializeEditorsPane();

        navigator.setBorder(null);
        editors.setBorder(null);

        navigator.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final TreePath path = navigator.getTree().getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        navigator.getTree().setSelectionPath(path);
                        final JPopupMenu menu = new JPopupMenu();
                        Actions.contribute(menu, "popup:navigator");
                        menu.show(navigator.getTree(), e.getX(), e.getY());
                    }
                }
            }
        });

        workspace.addProjectChangeListener(new ProjectChangeListener() {
            @Override
            public void projectAdded(@NotNull ProjectContainer container) {
                try {
                    final NavigatorTreeModel model = navigator.getModel();
                    final NavigatorWorkspaceNode workspaceNode = getWorkspaceNode();
                    final NavigatorProjectNode projectNode = new NavigatorProjectNode(workspaceNode, container);
                    final int childIndex = workspace.getProjects().indexOf(container);

                    workspaceNode.addChild(projectNode, childIndex);
                    model.fireNodesInserted(workspaceNode, childIndex);
                } catch (Exception e) {
                    log.error("Error reflecting project addition", e);
                }
            }

            @Override
            public void projectUpdated(@NotNull ProjectContainer container) {
                try {
                    final NavigatorTreeModel model = navigator.getModel();
                    final NavigatorProjectNode projectNode = getProjectNode(new VoidProgressMonitor(), container);

                    model.fireNodesChanged(projectNode);
                } catch (Exception e) {
                    log.error("Error reflecting project update", e);
                }
            }

            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                try {
                    final NavigatorTreeModel model = navigator.getModel();
                    final NavigatorWorkspaceNode workspaceNode = getWorkspaceNode();
                    final NavigatorProjectNode projectNode = getProjectNode(new VoidProgressMonitor(), container);
                    final int childIndex = model.getIndexOfChild(workspaceNode, projectNode);

                    workspaceNode.removeChild(childIndex);
                    model.fireNodesRemoved(workspaceNode, childIndex);
                } catch (Exception e) {
                    log.error("Error reflecting project removal", e);
                }
            }

            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                try {
                    final NavigatorTreeModel model = navigator.getModel();
                    final NavigatorProjectNode projectNode = getProjectNode(new VoidProgressMonitor(), container);

                    if (!projectNode.needsInitialization()) {
                        // TODO: This functionality should belong to the node itself
                        projectNode.clear();
                        navigator.getTree().collapsePath(new TreePath(model.getPathToRoot(projectNode)));
                        model.fireStructureChanged(projectNode);
                    }
                } catch (Exception e) {
                    log.error("Error reflecting project close", e);
                }
            }

            @NotNull
            private NavigatorProjectNode getProjectNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container) throws Exception {
                final NavigatorNode node = navigator.findChild(
                    monitor,
                    child -> child instanceof NavigatorProjectNode n && n.getContainer() == container
                );

                if (node != null) {
                    return (NavigatorProjectNode) node;
                } else {
                    throw new IllegalArgumentException("Can't find node for project " + container.getName() + " (" + container.getId() + ")");
                }
            }

            @NotNull
            private NavigatorWorkspaceNode getWorkspaceNode() {
                return (NavigatorWorkspaceNode) navigator.getModel().getRoot();
            }

        });

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.add(navigator);
        pane.add(editors);

        getContentPane().add(pane);

        SwingUtilities.invokeLater(() -> pane.setDividerLocation(0.25));
    }

    private void initializeEditorsPane() {
        editors.setBorder(new FlatBorder());
        editors.addEditorChangeListener(new EditorChangeListener() {
            @Override
            public void editorOpened(@NotNull Editor editor) {
                setTitle(getApplicationTitle());
            }

            @Override
            public void editorClosed(@NotNull Editor editor) {
                setTitle(getApplicationTitle());
            }
        });
    }

    private void initializeNavigatorPane() {
        final JTree tree = navigator.getTree();
        tree.setRootVisible(false);
        tree.setTransferHandler(new FileTransferHandler());
        tree.setDropTarget(null);
        tree.setDragEnabled(true);
    }

    @NotNull
    public Workspace getWorkspace() {
        return workspace;
    }

    @NotNull
    public NavigatorTree getNavigator() {
        return navigator;
    }

    @NotNull
    public EditorManager getEditorManager() {
        return editors;
    }

    @NotNull
    private String getApplicationTitle() {
        final Editor activeEditor = editors.getActiveEditor();
        if (activeEditor != null) {
            return Application.APPLICATION_TITLE + " - " + activeEditor.getInput().getName();
        } else {
            return Application.APPLICATION_TITLE;
        }
    }

    private void initializeMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        initializeFileMenu(menuBar);
        initializeEditMenu(menuBar);
        initializeHelpMenu(menuBar);

        setJMenuBar(menuBar);
    }

    private void initializeFileMenu(@NotNull JMenuBar menuBar) {
        final JMenu menuItemFile = new JMenu("File");
        menuItemFile.setMnemonic(KeyEvent.VK_F);

        Actions.contribute(menuItemFile, "menu:file");

        menuBar.add(menuItemFile);
    }

    private void initializeEditMenu(JMenuBar menuBar) {
        final JMenu menuItemEdit = new JMenu("Edit");
        menuItemEdit.setMnemonic(KeyEvent.VK_E);

        Actions.contribute(menuItemEdit, "menu:edit");

        menuBar.add(menuItemEdit);
    }

    private void initializeHelpMenu(JMenuBar menuBar) {
        final JMenu menuItemHelp = new JMenu("Help");
        menuItemHelp.setMnemonic(KeyEvent.VK_H);

        Actions.contribute(menuItemHelp, "menu:help");

        menuBar.add(menuItemHelp);
    }

    @Override
    public void dispose() {
        try {
            workspace.close();
        } catch (IOException e) {
            log.error("Error closing workspace", e);
        }

        super.dispose();
    }
}
