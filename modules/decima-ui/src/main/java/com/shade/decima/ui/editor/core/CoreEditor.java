package com.shade.decima.ui.editor.core;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.edit.MemoryChange;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.ValueController.EditType;
import com.shade.decima.ui.data.ValueViewer;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CoreEditor extends JSplitPane implements SaveableEditor, StatefulEditor {
    private static final DataKey<ValueViewer> VALUE_VIEWER_KEY = new DataKey<>("valueViewer", ValueViewer.class);

    private final ProjectEditorInput input;
    private final CoreBinary binary;
    private final MessageBusConnection connection;

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
        this(input, loadCoreBinary(input));
    }

    public CoreEditor(@NotNull NodeEditorInput input) {
        this(input, loadCoreBinary(input));
    }

    private CoreEditor(@NotNull ProjectEditorInput input, @NotNull CoreBinary binary) {
        this.input = input;
        this.binary = binary;
        this.connection = MessageBus.getInstance().connect();

        connection.subscribe(CoreEditorSettings.SETTINGS, () -> {
            final CoreEditorSettings settings = CoreEditorSettings.getInstance();
            if (breadcrumbBarPane.isVisible() != settings.showBreadcrumbs) {
                breadcrumbBarPane.setVisible(settings.showBreadcrumbs);
                revalidate();
            }
        });
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        final CoreNodeBinary root = new CoreNodeBinary(this);
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

        final JScrollPane propertiesTreePane = new JScrollPane(tree);
        propertiesTreePane.setBorder(null);

        final CoreEditorSettings settings = CoreEditorSettings.getInstance();

        breadcrumbBarPane = new JScrollPane(new BreadcrumbBar(tree));
        breadcrumbBarPane.setVisible(settings.showBreadcrumbs);
        breadcrumbBarPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.shadow")));
        breadcrumbBarPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        breadcrumbBarPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(propertiesTreePane, BorderLayout.CENTER);
        mainPanel.add(breadcrumbBarPane, BorderLayout.SOUTH);

        setLeftComponent(mainPanel);
        setRightComponent(null);
        setResizeWeight(1.0);
        setOneTouchExpandable(true);

        if (selectionPath != null) {
            setSelectionPath(selectionPath);
        } else if (settings.selectFirstEntry && !binary.isEmpty()) {
            setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(binary.entries().get(0))));
        }

        updateCurrentViewer();

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
    public <T> ValueController<T> getValueController(@NotNull EditType type) {
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

    @SuppressWarnings({"unchecked"})
    @Override
    public void loadState(@NotNull Map<String, Object> state) {
        final var selection = (List<Map<String, Object>>) state.get("selection");

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

            final CoreNodeBinary root = (CoreNodeBinary) tree.getModel().getRoot();
            if (root.isGroupingEnabled()) {
                state.put("group", Boolean.TRUE);
            }
            if (root.isSortingEnabled()) {
                state.put("sort", Boolean.TRUE);
            }
        } else {
            if (selectionPath != null) {
                state.put("selection", serializePath(selectionPath));
            }
            if (groupingEnabled) {
                state.put("group", Boolean.TRUE);
            }
            if (sortingEnabled) {
                state.put("sort", Boolean.TRUE);
            }
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

        final byte[] serialized = binary.serialize(input.getProject().getTypeRegistry());

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

        connection.dispose();
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
    public CoreBinary getBinary() {
        return binary;
    }

    private void fireDirtyStateChange() {
        firePropertyChange("dirty", null, isDirty());
    }

    private void updateCurrentViewer() {
        final JComponent currentComponent = (JComponent) getRightComponent();
        final ValueViewer currentViewer = currentComponent != null ? VALUE_VIEWER_KEY.get(currentComponent) : null;

        if (tree.getLastSelectedPathComponent() instanceof CoreNodeObject node) {
            final CoreValueController<Object> controller = new CoreValueController<>(this, node, EditType.INLINE);
            final ValueViewer viewer = ValueRegistry.getInstance().findViewer(controller);

            if (viewer != null && viewer.canView(controller)) {
                final boolean viewerChanged = currentViewer != viewer;
                final JComponent component;

                if (viewerChanged) {
                    if (currentComponent instanceof Disposable d) {
                        d.dispose();
                    }

                    component = viewer.createComponent();
                    component.putClientProperty(VALUE_VIEWER_KEY, viewer);
                    setRightComponent(component);
                } else {
                    component = currentComponent;
                }

                viewer.refresh(component, controller);

                if (viewerChanged) {
                    fitValueViewer(component);

                    if (!CoreEditorSettings.getInstance().showValuePanel) {
                        UIUtils.minimizePanel(this, false);
                    }
                }

                return;
            }
        }

        if (currentComponent instanceof Disposable d) {
            d.dispose();
        }

        setRightComponent(null);
    }

    private void fitValueViewer(@NotNull JComponent component) {
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
    private static CoreBinary loadCoreBinary(@NotNull NodeEditorInput input) {
        try {
            return CoreBinary.from(
                input.getNode().getPackfile().extract(input.getNode().getHash()),
                input.getProject().getTypeRegistry(),
                true
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private static CoreBinary loadCoreBinary(@NotNull FileEditorInput input) {
        try {
            return CoreBinary.from(
                Files.readAllBytes(input.getPath()),
                input.getProject().getTypeRegistry(),
                true
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private static List<Map<String, Object>> serializePath(@NotNull RTTIPath path) {
        final List<Map<String, Object>> selection = new ArrayList<>();

        for (RTTIPathElement element : path.elements()) {
            if (element instanceof RTTIPathElement.Field e) {
                selection.add(Map.of("type", "field", "value", e.name()));
            } else if (element instanceof RTTIPathElement.Index e) {
                selection.add(Map.of("type", "index", "value", e.index()));
            } else if (element instanceof RTTIPathElement.UUID e) {
                selection.add(Map.of("type", "uuid", "value", e.uuid()));
            }
        }

        return selection;
    }

    @NotNull
    private static RTTIPath deserializePath(@NotNull List<Map<String, Object>> object) {
        final List<RTTIPathElement> elements = new ArrayList<>();

        for (Map<String, Object> element : object) {
            final RTTIPathElement result = switch ((String) element.get("type")) {
                case "field" -> new RTTIPathElement.Field((String) element.get("value"));
                case "index" -> new RTTIPathElement.Index(((Number) element.get("value")).intValue());
                case "uuid" -> new RTTIPathElement.UUID((String) element.get("value"));
                default -> throw new IllegalArgumentException("Unexpected element: " + element);
            };

            elements.add(result);
        }

        return new RTTIPath(elements.toArray(RTTIPathElement[]::new));
    }
}
