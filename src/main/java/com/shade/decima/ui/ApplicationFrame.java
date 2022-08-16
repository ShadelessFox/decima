package com.shade.decima.ui;

import com.shade.decima.model.app.DataContext;
import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.model.app.runtime.ProgressMonitor;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.util.IOUtils;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorChangeListener;
import com.shade.decima.ui.editor.EditorInput;
import com.shade.decima.ui.editor.EditorManager;
import com.shade.decima.ui.editor.lazy.LazyEditorInput;
import com.shade.decima.ui.editor.stack.EditorStackManager;
import com.shade.decima.ui.menu.MenuConstants;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final NavigatorTree navigator;
    private final EditorStackManager editors;

    public ApplicationFrame() {
        try {
            this.workspace = new Workspace();
            this.navigator = new NavigatorTree(new NavigatorWorkspaceNode(workspace));
            this.editors = new EditorStackManager(workspace);

            setTitle(getApplicationTitle());

            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                final EditorManager manager = getEditorManager();
                final Preferences editors = workspace.getPreferences().node("editors");
                final int selection = editors.getInt("selection", 0);

                IOUtils.forEach(editors, (name, pref) -> {
                    final String project = IOUtils.getNotNull(pref, "project");
                    final String packfile = IOUtils.getNotNull(pref, "packfile");
                    final String resource = IOUtils.getNotNull(pref, "resource");
                    final boolean select = manager.getEditorsCount() == selection;
                    manager.openEditor(new LazyEditorInput(project, packfile, resource), select, false);
                });
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    workspace.getPreferences().node("editors").removeNode();
                } catch (BackingStoreException ex) {
                    log.warn("Unable to clear last opened editors", ex);
                }

                final Preferences root = workspace.getPreferences().node("editors");
                final Editor[] editors = getEditorManager().getEditors();
                final Editor activeEditor = getEditorManager().getActiveEditor();

                for (int i = 0, index = 0; i < editors.length; i++) {
                    final Editor editor = editors[i];
                    final EditorInput input = editor.getInput();

                    if (editor == activeEditor) {
                        root.putInt("selection", index);
                    }

                    final Preferences pref = root.node(String.valueOf(index++));
                    final String project;
                    final String packfile;
                    final String resource;

                    if (input instanceof LazyEditorInput lazy) {
                        project = lazy.container().toString();
                        packfile = lazy.packfile();
                        resource = lazy.path().full();
                    } else {
                        project = input.getProject().getContainer().getId().toString();
                        packfile = UIUtils.getPackfile(input.getNode()).getPath().getFileName().toString();
                        resource = input.getNode().getPath().full();
                    }

                    pref.put("project", project);
                    pref.put("packfile", packfile);
                    pref.put("resource", resource);
                }
            }
        });
    }

    private void initialize() {
        initializeNavigatorPane();
        initializeEditorsPane();

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
                        navigator.collapsePath(new TreePath(model.getPathToRoot(projectNode)));
                        model.fireStructureChanged(projectNode);
                    }
                } catch (Exception e) {
                    log.error("Error reflecting project close", e);
                }
            }

            @NotNull
            private NavigatorProjectNode getProjectNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container) throws Exception {
                final NavigatorNode node = navigator
                    .findChild(monitor, child -> child instanceof NavigatorProjectNode n && n.getContainer() == container)
                    .get();

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

        final JScrollPane navigatorPane = new JScrollPane(navigator);
        navigatorPane.setBorder(null);

        final JTabbedPane leftPane = new JTabbedPane();
        leftPane.setFocusable(false);
        leftPane.add("Projects", navigatorPane);

        final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.setUI(new ThinFlatSplitPaneUI());
        pane.setLeftComponent(leftPane);
        pane.setRightComponent(editors);

        setContentPane(pane);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                pane.setDividerLocation(0.25);
                removeComponentListener(this);
            }
        });

        final NavigatorContext context = new NavigatorContext();
        UIUtils.installPopupMenu(
            navigator,
            Application.getMenuService().createContextMenu(navigator, MenuConstants.CTX_MENU_NAVIGATOR_ID, context)
        );
        Application.getMenuService().createContextMenuKeyBindings(
            navigator,
            MenuConstants.CTX_MENU_NAVIGATOR_ID,
            context
        );
    }

    private void initializeEditorsPane() {
        editors.addEditorChangeListener(new EditorChangeListener() {
            @Override
            public void editorChanged(@Nullable Editor editor) {
                setTitle(getApplicationTitle());
            }
        });
    }

    private void initializeNavigatorPane() {
        navigator.setRootVisible(false);
        navigator.setTransferHandler(new FileTransferHandler());
        navigator.setDropTarget(null);
        navigator.setDragEnabled(true);
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

    @Override
    public void dispose() {
        try {
            workspace.close();
        } catch (IOException e) {
            log.error("Error closing workspace", e);
        }

        super.dispose();
    }

    private class NavigatorContext implements DataContext {
        @Override
        public Object getData(@NotNull String key) {
            return switch (key) {
                case "workspace" -> workspace;
                case "selection" -> navigator.getLastSelectedPathComponent();
                case "project" -> {
                    final NavigatorNode node = (NavigatorNode) navigator.getLastSelectedPathComponent();
                    final NavigatorProjectNode parent = UIUtils.findParentNode(node, NavigatorProjectNode.class);
                    yield parent != null && !parent.needsInitialization() ? parent.getProject() : null;
                }
                case "projectContainer" -> {
                    final NavigatorNode node = (NavigatorNode) navigator.getLastSelectedPathComponent();
                    final NavigatorProjectNode parent = UIUtils.findParentNode(node, NavigatorProjectNode.class);
                    yield parent != null ? parent.getContainer() : null;
                }
                default -> null;
            };
        }
    }
}
