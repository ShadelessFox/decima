package com.shade.decima.ui.editor.core;

import com.shade.decima.model.app.ProjectPersister;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.resource.Resource;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.core.command.AttributeChangeCommand;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.commands.Command;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.platform.ui.commands.CommandManagerChangeListener;
import com.shade.platform.ui.controls.BreadcrumbBar;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

public class CoreEditor extends JSplitPane implements SaveableEditor {
    private final FileEditorInput input;
    private final Tree tree;
    private final CommandManager commandManager;

    private ValueViewer activeValueViewer;

    public CoreEditor(@NotNull FileEditorInput input) {
        final CoreNodeBinary root = new CoreNodeBinary(createCoreBinary(input));

        this.input = input;
        this.tree = new Tree(root, CoreTreeModel::new);
        this.tree.setCellRenderer(new CoreTreeCellRenderer(tree.getModel()));
        this.tree.setCellEditor(new CoreTreeCellEditor(this));
        this.tree.setEditable(true);
        this.tree.setSelectionPath(new TreePath(root));
        this.tree.addTreeSelectionListener(e -> updateCurrentViewer());
        this.tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tree.startEditingAtPath(tree.getSelectionPath());
                }
            }
        });

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

        final EditorContext context = new EditorContext();
        UIUtils.installPopupMenu(
            tree,
            Application.getMenuService().createContextMenu(tree, MenuConstants.CTX_MENU_CORE_EDITOR_ID, context)
        );
        Application.getMenuService().createContextMenuKeyBindings(
            tree,
            MenuConstants.CTX_MENU_CORE_EDITOR_ID,
            context
        );

        final JScrollPane propertiesTreePane = new JScrollPane(tree);
        propertiesTreePane.setBorder(null);

        final BreadcrumbBar breadcrumbBar = new BreadcrumbBar(tree.getSelectionModel());
        breadcrumbBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(propertiesTreePane, BorderLayout.CENTER);
        mainPanel.add(breadcrumbBar, BorderLayout.SOUTH);

        setLeftComponent(mainPanel);
        setRightComponent(null);
        setResizeWeight(0.75);
        setOneTouchExpandable(true);

        updateCurrentViewer();
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return this;
    }

    @NotNull
    @Override
    public FileEditorInput getInput() {
        return input;
    }

    @Nullable
    public RTTIType<?> getSelectedType() {
        if (tree.getLastSelectedPathComponent() instanceof CoreNodeObject node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    public Object getSelectedValue() {
        if (tree.getLastSelectedPathComponent() instanceof CoreNodeObject node) {
            return node.getObject();
        }
        return null;
    }

    public void setSelectedValue(@Nullable Object value) {
        if (value instanceof RTTIObject object && object.getType().isInstanceOf("GGUUID")) {
            tree.getModel()
                .findChild(new VoidProgressMonitor(), child -> child instanceof CoreNodeEntry entry && entry.getObjectUUID().equals(object))
                .whenComplete((node, exception) -> {
                    if (exception != null) {
                        UIUtils.showErrorDialog(exception);
                        return;
                    }

                    if (node != null) {
                        final TreePath path = new TreePath(tree.getModel().getPathToRoot(node));
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                    }
                });

        }
    }

    @Override
    public void setFocus() {
        tree.requestFocusInWindow();
    }

    @Override
    public boolean isDirty() {
        return commandManager.canUndo();
    }

    @Override
    public void doSave(@NotNull ProgressMonitor monitor) {
        final CoreBinary binary = createCoreBinary(input);

        for (Command command : commandManager.getMergedCommands()) {
            if (command instanceof AttributeChangeCommand c) {
                c.getNode().getPath().set(binary, c.getNewValue());
            }
        }

        final InMemoryChange change = new InMemoryChange(
            binary.serialize(input.getProject().getTypeRegistry()),
            input.getNode().getHash()
        );

        input.getProject().getPersister().addChange(input.getNode(), change);
        commandManager.discardAllCommands();
        fireDirtyStateChange();
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
        return commandManager;
    }

    @NotNull
    public Tree getTree() {
        return tree;
    }

    @NotNull
    private CoreBinary createCoreBinary(@NotNull FileEditorInput input) {
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

    private void fireDirtyStateChange() {
        firePropertyChange("dirty", null, isDirty());
    }

    private void updateCurrentViewer() {
        final RTTIType<?> type = getSelectedType();

        if (type != null) {
            final ValueViewer viewer = ValueEditorProvider.findValueViewer(type);

            if (viewer != null) {
                if (activeValueViewer != viewer) {
                    activeValueViewer = viewer;
                    setRightComponent(viewer.createComponent());
                }

                activeValueViewer.refresh((JComponent) getRightComponent(), this);
                return;
            }
        }

        activeValueViewer = null;
        setRightComponent(null);
    }

    private class EditorContext implements DataContext {
        @Override
        public Object getData(@NotNull String key) {
            return switch (key) {
                case "editor" -> CoreEditor.this;
                case "selection" -> tree.getLastSelectedPathComponent();
                default -> null;
            };
        }
    }

    private static record InMemoryChange(@NotNull byte[] data, long hash) implements ProjectPersister.Change {
        @NotNull
        @Override
        public ProjectPersister.Change merge(@NotNull ProjectPersister.Change change) {
            throw new IllegalArgumentException("Can't merge with " + change);
        }

        @NotNull
        @Override
        public Resource toResource() {
            return new BufferResource(data, hash);
        }
    }

    private static class BufferResource implements Resource {
        private final byte[] data;
        private final long hash;
        private int position;

        public BufferResource(@NotNull byte[] data, long hash) {
            this.data = data;
            this.hash = hash;
            this.position = 0;
        }

        @Override
        public long read(@NotNull ByteBuffer buffer) throws IOException {
            final int length = Math.min(data.length - position, buffer.remaining());
            buffer.put(data, position, length);
            position += length;
            return length;
        }

        @Override
        public long hash() {
            return hash;
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public void close() {
            // nothing to close
        }
    }
}
