package com.shade.decima.ui;

import com.shade.decima.BuildConfig;
import com.shade.decima.model.app.ProjectChangeListener;
import com.shade.decima.model.app.ProjectContainer;
import com.shade.decima.model.app.Workspace;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.FileEditorInputLazy;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.dnd.FileTransferHandler;
import com.shade.decima.ui.navigator.impl.NavigatorNode;
import com.shade.decima.ui.navigator.impl.NavigatorProjectNode;
import com.shade.decima.ui.navigator.impl.NavigatorWorkspaceNode;
import com.shade.decima.ui.navigator.menu.ProjectCloseItem;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.controls.plaf.ThinFlatSplitPaneUI;
import com.shade.platform.ui.controls.tree.TreeModel;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorChangeListener;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.editors.stack.EditorStack;
import com.shade.platform.ui.editors.stack.EditorStackContainer;
import com.shade.platform.ui.editors.stack.EditorStackManager;
import com.shade.platform.ui.menus.MenuService;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApplicationFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(ApplicationFrame.class);

    private final Workspace workspace;
    private final NavigatorTree navigator;
    private final EditorStackManager editors;

    public ApplicationFrame() {
        this.workspace = new Workspace();
        this.navigator = new NavigatorTree(new NavigatorWorkspaceNode(workspace));
        this.editors = new MyEditorStackManager(null);

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
                    manager.openEditor(new FileEditorInputLazy(project, packfile, resource), select, false);
                });
            }

            @Override
            public void windowClosing(WindowEvent e) {
                for (ProjectContainer container : workspace.getProjects()) {
                    final NavigatorProjectNode node = getProjectNode(new VoidProgressMonitor(), container);
                    if (!node.needsInitialization() && !ProjectCloseItem.confirmProjectClose(node.getProject(), editors)) {
                        return;
                    }
                }

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

                    final String project;
                    final String packfile;
                    final String resource;

                    if (editor.getInput() instanceof FileEditorInputLazy input) {
                        project = input.container().toString();
                        packfile = input.packfile();
                        resource = input.path().full();
                    } else if (editor.getInput() instanceof FileEditorInput input) {
                        project = input.getProject().getContainer().getId().toString();
                        packfile = input.getNode().getPackfile().getPath().getFileName().toString();
                        resource = input.getNode().getPath().full();
                    } else {
                        continue;
                    }

                    if (editor == activeEditor) {
                        root.putInt("selection", index);
                    }

                    final Preferences pref = root.node(String.valueOf(index++));
                    pref.put("project", project);
                    pref.put("packfile", packfile);
                    pref.put("resource", resource);
                }

                System.exit(0);
            }
        });

        initialize();

        setTitle(getApplicationTitle());
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }

    private void initialize() {
        initializeNavigatorPane();
        initializeEditorsPane();

        workspace.addProjectChangeListener(new ProjectChangeListener() {
            @Override
            public void projectAdded(@NotNull ProjectContainer container) {
                final TreeModel model = navigator.getModel();
                final NavigatorWorkspaceNode workspaceNode = getWorkspaceNode();
                final NavigatorProjectNode projectNode = new NavigatorProjectNode(workspaceNode, container);
                final int childIndex = workspace.getProjects().indexOf(container);

                workspaceNode.addChild(projectNode, childIndex);
                model.fireNodesInserted(workspaceNode, childIndex);
            }

            @Override
            public void projectUpdated(@NotNull ProjectContainer container) {
                final TreeModel model = navigator.getModel();
                final NavigatorProjectNode projectNode = getProjectNode(new VoidProgressMonitor(), container);
                projectNode.resetIcon();

                model.fireNodesChanged(projectNode);
            }

            @Override
            public void projectRemoved(@NotNull ProjectContainer container) {
                final TreeModel model = navigator.getModel();
                final NavigatorWorkspaceNode workspaceNode = getWorkspaceNode();
                final NavigatorProjectNode projectNode = getProjectNode(new VoidProgressMonitor(), container);
                final int childIndex = model.getIndexOfChild(workspaceNode, projectNode);

                workspaceNode.removeChild(childIndex);
                model.fireNodesRemoved(workspaceNode, childIndex);
            }

            @Override
            public void projectClosed(@NotNull ProjectContainer container) {
                for (Editor editor : editors.getEditors()) {
                    if (editor.getInput() instanceof FileEditorInput input && input.getProject().getContainer().equals(container)) {
                        editors.closeEditor(editor);
                    }

                    if (editor.getInput() instanceof FileEditorInputLazy input && input.container().equals(container.getId())) {
                        editors.closeEditor(editor);
                    }
                }

                navigator.getModel().unloadNode(getProjectNode(new VoidProgressMonitor(), container));
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
            return BuildConfig.APP_TITLE + " - " + activeEditor.getInput().getName();
        } else {
            return BuildConfig.APP_TITLE;
        }
    }

    @Override
    public void dispose() {
        workspace.close();
        super.dispose();
    }

    @NotNull
    private NavigatorProjectNode getProjectNode(@NotNull ProgressMonitor monitor, @NotNull ProjectContainer container) {
        final TreeNode node;

        try {
            node = navigator
                .getModel().findChild(monitor, child -> child instanceof NavigatorProjectNode n && n.getProjectContainer() == container)
                .get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while looking for node of project " + container.getName() + " (" + container.getId() + ")", e);
        }

        if (node != null) {
            return (NavigatorProjectNode) node;
        } else {
            throw new IllegalArgumentException("Can't find node for project " + container.getName() + " (" + container.getId() + ")");
        }
    }

    private class NavigatorContext implements DataContext {
        @Override
        public Object getData(@NotNull String key) {
            return switch (key) {
                case "workspace" -> workspace;
                case "selection" -> navigator.getLastSelectedPathComponent();
                case "project" -> {
                    final NavigatorNode node = (NavigatorNode) navigator.getLastSelectedPathComponent();
                    final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
                    yield parent != null && !parent.needsInitialization() ? parent.getProject() : null;
                }
                case "projectContainer" -> {
                    final NavigatorNode node = (NavigatorNode) navigator.getLastSelectedPathComponent();
                    final NavigatorProjectNode parent = node.findParentOfType(NavigatorProjectNode.class);
                    yield parent != null ? parent.getProjectContainer() : null;
                }
                case "editorManager" -> editors;
                default -> null;
            };
        }
    }

    private class MyEditorStackManager extends EditorStackManager {
        public MyEditorStackManager(@Nullable Component component) {
            super(component);
        }

        @Override
        public void closeEditor(@NotNull Editor editor) {
            if (editor instanceof SaveableEditor e && e.isDirty()) {
                final int result = JOptionPane.showConfirmDialog(
                    this,
                    "Editor for file '%s' has unsaved changes.\n\nAre you sure you want to close it?".formatted(editor.getInput().getName()),
                    "Confirm Close",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            super.closeEditor(editor);
        }

        @NotNull
        @Override
        protected EditorStack createEditorStack() {
            final EditorStack stack = super.createEditorStack();
            final DataContext context = key -> switch (key) {
                case "editor" -> editors.getActiveEditor();
                case "editorStack" -> stack;
                case "editorManager" -> editors;
                default -> null;
            };

            final MenuService menuService = Application.getMenuService();
            UIUtils.installPopupMenu(stack, menuService.createContextMenu(stack, MenuConstants.CTX_MENU_EDITOR_STACK_ID, context));
            menuService.createContextMenuKeyBindings(stack, MenuConstants.CTX_MENU_EDITOR_STACK_ID, context);

            return stack;
        }

        @NotNull
        @Override
        protected EditorStackContainer createEditorStackContainer(@Nullable Component component) {
            return new MyEditorStackManager(component);
        }
    }
}
