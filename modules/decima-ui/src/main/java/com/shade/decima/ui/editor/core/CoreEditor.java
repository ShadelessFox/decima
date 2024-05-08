package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.packfile.edit.MemoryChange;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.data.MutableValueController;
import com.shade.decima.ui.data.MutableValueController.EditType;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.data.ValueViewerPanel;
import com.shade.decima.ui.data.registry.ValueRegistry;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.NodeEditorInput;
import com.shade.decima.ui.editor.ProjectEditorInput;
import com.shade.decima.ui.editor.core.settings.CoreEditorSettings;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.commands.Command;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.platform.ui.commands.CommandManagerChangeListener;
import com.shade.platform.ui.controls.BreadcrumbBar;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.editors.StatefulEditor;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class CoreEditor extends JSplitPane implements SaveableEditor, StatefulEditor {
    private static final DataKey<ValueViewer> VALUE_VIEWER_KEY = new DataKey<>("valueViewer", ValueViewer.class);

    private final ProjectEditorInput input;
    private final RTTICoreFile file;
    private final MessageBusConnection connection;
    private final int errors;

    // Initialized in CoreEditor#createComponent
    private CoreTree tree;
    private JScrollPane breadcrumbBarPane;
    private CommandManager commandManager;
    private boolean dirty;

    // Initialized in CoreEditor#loadState
    private RTTIPath selectionPath;
    private boolean groupingEnabled;
    private boolean sortingEnabled;

    public CoreEditor(@NotNull FileEditorInput input) {
        this(input, loadFile(input));
    }

    public CoreEditor(@NotNull NodeEditorInput input) {
        this(input, loadFile(input));
    }

    private CoreEditor(@NotNull ProjectEditorInput input, @NotNull FileLoadResult result) {
        this.input = input;
        this.file = result.file;
        this.errors = result.errors;
        this.connection = MessageBus.getInstance().connect();

        connection.subscribe(CoreEditorSettings.SETTINGS, () -> {
            final CoreEditorSettings settings = CoreEditorSettings.getInstance();
            if (breadcrumbBarPane.isVisible() != settings.showBreadcrumbs) {
                breadcrumbBarPane.setVisible(settings.showBreadcrumbs);
                revalidate();
            }
            // In case any of presentation attributes were changed
            tree.getUI().invalidateSizes();
        });

        final CoreEditorSettings settings = CoreEditorSettings.getInstance();
        groupingEnabled = settings.groupEntries;
        sortingEnabled = settings.sortEntries;
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final CoreNodeFile root = new CoreNodeFile(this);
        root.setGroupingEnabled(groupingEnabled);
        root.setSortingEnabled(sortingEnabled);

        tree = new CoreTree(root);
        tree.setCellEditor(new CoreTreeCellEditor(this));
        tree.setEditable(true);
        tree.addTreeSelectionListener(e -> updateCurrentViewer());
        tree.setTransferHandler(new CoreTreeTransferHandler(this));
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);

        commandManager = new CommandManager();
        commandManager.addChangeListener(new CommandManagerChangeListener() {
            @Override
            public void commandDidRedo(@NotNull Command command) {
                fireDirtyStateChange();
            }

            @Override
            public void commandDidUndo(@NotNull Command command) {
                fireDirtyStateChange();
            }
        });

        final CoreEditorSettings settings = CoreEditorSettings.getInstance();

        breadcrumbBarPane = new JScrollPane(new BreadcrumbBar(tree));
        breadcrumbBarPane.setVisible(settings.showBreadcrumbs);
        breadcrumbBarPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIColor.SHADOW));
        breadcrumbBarPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        breadcrumbBarPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(UIUtils.createBorderlessScrollPane(tree), BorderLayout.CENTER);
        mainPanel.add(breadcrumbBarPane, BorderLayout.SOUTH);

        setLeftComponent(mainPanel);
        setRightComponent(null);
        setResizeWeight(1.0);
        setOneTouchExpandable(true);

        if (selectionPath != null) {
            setSelectionPath(selectionPath);
        } else if (settings.selectFirstEntry && !file.objects().isEmpty()) {
            final RTTIObject object;

            if (sortingEnabled) {
                object = file.objects().stream()
                    .min(Comparator.comparing(entry -> entry.type().getTypeName()))
                    .orElseThrow();
            } else {
                object = file.objects().get(0);
            }

            setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(object)));
        }

        MenuManager.getInstance().installContextMenu(tree, MenuConstants.CTX_MENU_CORE_EDITOR_ID, key -> switch (key) {
            case "editor" -> this;
            case "selection" -> tree.getLastSelectedPathComponent();
            case "project" -> input.getProject();
            case "commandManager" -> commandManager;
            default -> null;
        });

        return this;
    }

    @NotNull
    @Override
    public ProjectEditorInput getInput() {
        return input;
    }

    @Nullable
    public <T> ValueController<T> getValueController() {
        return getValueController(EditType.INLINE);
    }

    @Nullable
    public <T> MutableValueController<T> getValueController(@NotNull EditType type) {
        if (tree.getLastSelectedPathComponent() instanceof CoreNodeObject node) {
            return new CoreValueController<>(this, node, type);
        } else {
            return null;
        }
    }

    public void setSelectionPath(@NotNull RTTIPath pathToSelect) {
        tree.getModel()
            .findNode(new VoidProgressMonitor(), pathToSelect)
            .whenComplete((node, exception) -> {
                if (exception != null) {
                    UIUtils.showErrorDialog(exception);
                    return;
                }

                if (node != null) {
                    final TreePath path = tree.getModel().getTreePathToRoot(node);
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                }
            });
    }

    @Override
    public void setFocus() {
        if (tree != null) {
            tree.requestFocusInWindow();
        }
    }

    @Override
    public void loadState(@NotNull Map<String, Object> state) {
        final var selection = state.get("selection");

        if (selection != null) {
            selectionPath = deserializePath(selection);
        }

        groupingEnabled = state.get("group") == Boolean.TRUE;
        sortingEnabled = state.get("sort") == Boolean.TRUE;
    }

    @Override
    public void saveState(@NotNull Map<String, Object> state) {
        if (tree != null) {
            final TreePath path = tree.getSelectionPath();

            if (path != null) {
                for (int i = path.getPathCount() - 1; i >= 0; i--) {
                    if (path.getPathComponent(i) instanceof CoreNodeObject obj) {
                        state.put("selection", serializePath(obj.getPath()));
                        break;
                    }
                }
            }

            final CoreNodeFile root = (CoreNodeFile) tree.getModel().getRoot();
            state.put("group", root.isGroupingEnabled());
            state.put("sort", root.isSortingEnabled());
        } else {
            if (selectionPath != null) {
                state.put("selection", serializePath(selectionPath));
            }

            state.put("group", groupingEnabled);
            state.put("sort", sortingEnabled);
        }
    }

    @Override
    public boolean isFocused() {
        return tree != null && tree.isFocusOwner();
    }

    @Override
    public boolean isDirty() {
        return dirty || commandManager != null && commandManager.canUndo();
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        firePropertyChange("dirty", null, isDirty());
    }

    @Override
    public void doSave(@NotNull ProgressMonitor monitor) {
        if (!isDirty()) {
            return;
        }

        final Project project = input.getProject();
        final byte[] serialized = project.getCoreFileReader().write(file);

        if (input instanceof NodeEditorInput i) {
            final NavigatorFileNode node = i.getNode();
            final MemoryChange change = new MemoryChange(serialized, node.getHash());
            node.getPackfile().addChange(node.getPath(), change);
        } else if (input instanceof FileEditorInput i) {
            try {
                Files.write(i.getPath(), serialized);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new IllegalArgumentException("Unexpected editor input: " + input);
        }

        commandManager.discardAllCommands();
        setDirty(false);
    }

    @Override
    public void doReset() {
        if (commandManager == null || !commandManager.canUndo()) {
            return;
        }

        commandManager.undoAllCommands();
        commandManager.discardAllCommands();

        setDirty(false);
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
    }

    @NotNull
    @Override
    public CommandManager getCommandManager() {
        return Objects.requireNonNull(commandManager, "Editor is not activated");
    }

    @Override
    public void dispose() {
        if (getRightComponent() instanceof Disposable d) {
            d.dispose();
        }

        setLeftComponent(null);
        setRightComponent(null);

        connection.dispose();
        tree.setCellEditor(null);
        tree.setTransferHandler(null);
    }

    @NotNull
    public Tree getTree() {
        return Objects.requireNonNull(tree, "Editor is not activated");
    }

    @NotNull
    public BreadcrumbBar getBreadcrumbBar() {
        return (BreadcrumbBar) Objects.requireNonNull(breadcrumbBarPane, "Editor is not activated").getViewport().getView();
    }

    @NotNull
    public RTTICoreFile getCoreFile() {
        return file;
    }

    public int getErrorCount() {
        return errors;
    }

    private void fireDirtyStateChange() {
        firePropertyChange("dirty", null, isDirty());
    }

    private void updateCurrentViewer() {
        ValueViewerPanel panel = (ValueViewerPanel) getRightComponent();

        if (tree.getLastSelectedPathComponent() instanceof CoreNodeObject node) {
            final CoreValueController<Object> controller = new CoreValueController<>(this, node, EditType.INLINE);
            final ValueViewer viewer = ValueRegistry.getInstance().findViewer(controller);

            if (viewer != null && viewer.canView(controller)) {
                if (panel == null) {
                    panel = new ValueViewerPanel();
                    setRightComponent(panel);
                }

                panel.update(viewer, controller, new ValueViewerPanel.Callback() {
                    @Override
                    public void viewerChanged(@NotNull ValueViewerPanel panel) {
                        fitValueViewer(panel);
                    }

                    @Override
                    public void viewerClosed(@NotNull ValueViewerPanel panel) {
                        panel.dispose();
                        setRightComponent(null);
                    }
                });

                return;
            }
        }

        if (panel != null) {
            panel.dispose();
        }

        setRightComponent(null);
    }

    private void fitValueViewer(@NotNull Component component) {
        final Dimension size = component.getPreferredSize();

        if (component instanceof JScrollPane pane) {
            if (pane.getHorizontalScrollBar().isVisible()) {
                size.height += pane.getHorizontalScrollBar().getHeight();
            }

            if (pane.getVerticalScrollBar().isVisible()) {
                size.width += pane.getVerticalScrollBar().getWidth();
            }
        }

        if (getOrientation() == HORIZONTAL_SPLIT) {
            setDividerLocation(getWidth() - size.width - getDividerSize());
        } else {
            setDividerLocation(getHeight() - size.height - getDividerSize());
        }
    }

    @NotNull
    private static FileLoadResult loadFile(@NotNull NodeEditorInput input) {
        try (InputStream is = input.getNode().getFile().newInputStream()) {
            return loadFile(input.getProject(), is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private static FileLoadResult loadFile(@NotNull FileEditorInput input) {
        try (InputStream is = Files.newInputStream(input.getPath())) {
            return loadFile(input.getProject(), is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private static FileLoadResult loadFile(@NotNull Project project, @NotNull InputStream is) throws IOException {
        final MetricLoggingErrorHandlingStrategy strategy = new MetricLoggingErrorHandlingStrategy();
        final RTTICoreFile file = project.getCoreFileReader().read(is, strategy);
        return new FileLoadResult(file, strategy.errors);
    }

    @NotNull
    private static String serializePath(@NotNull RTTIPath path) {
        final StringBuilder selection = new StringBuilder();

        for (RTTIPathElement element : path.elements()) {
            if (element instanceof RTTIPathElement.Field e) {
                selection.append('.').append(e.name());
            } else if (element instanceof RTTIPathElement.Index e) {
                selection.append('[').append(e.index()).append(']');
            } else if (element instanceof RTTIPathElement.UUID e) {
                selection.append('{').append(e.uuid()).append('}');
            }
        }

        return selection.toString();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static RTTIPath deserializePath(@NotNull Object object) {
        final List<RTTIPathElement> elements = new ArrayList<>();

        if (object instanceof List) {
            for (Map<String, Object> element : (List<Map<String, Object>>) object) {
                final RTTIPathElement result = switch ((String) element.get("type")) {
                    case "field" -> new RTTIPathElement.Field((String) element.get("value"));
                    case "index" -> new RTTIPathElement.Index(((Number) element.get("value")).intValue());
                    case "uuid" -> new RTTIPathElement.UUID((String) element.get("value"));
                    default -> throw new IllegalArgumentException("Unexpected element: " + element);
                };

                elements.add(result);
            }
        } else {
            final String string = (String) object;

            for (int i = 0; i < string.length(); ) {
                final RTTIPathElement element = switch (string.charAt(i)) {
                    case '{' -> {
                        final int to = string.indexOf('}', i);
                        final String uuid = string.substring(i + 1, to);
                        i = to + 1;

                        yield new RTTIPathElement.UUID(uuid);
                    }
                    case '[' -> {
                        final int to = string.indexOf(']', i);
                        final int index = Integer.parseInt(string.substring(i + 1, to));
                        i = to + 1;

                        yield new RTTIPathElement.Index(index);
                    }
                    case '.' -> {
                        final int start = ++i;

                        for (; i < string.length(); i++) {
                            final char ch = string.charAt(i);
                            if (ch == '{' || ch == '[' || ch == '.') {
                                break;
                            }
                        }

                        yield new RTTIPathElement.Field(string.substring(start, i));
                    }
                    default ->
                        throw new IllegalArgumentException("Unexpected character '" + string.charAt(i) + "', was expecting '[', '{', or '.'");
                };

                elements.add(element);
            }
        }

        return new RTTIPath(elements.toArray(RTTIPathElement[]::new));
    }

    private record FileLoadResult(@NotNull RTTICoreFile file, int errors) {}

    private static class MetricLoggingErrorHandlingStrategy extends LoggingErrorHandlingStrategy {
        private int errors = 0;

        @Override
        public void handle(@NotNull Exception e) {
            super.handle(e);
            errors++;
        }
    }
}
