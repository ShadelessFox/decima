package com.shade.decima.ui.editor.property;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.NavigatorEditorInput;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.commands.Command;
import com.shade.platform.ui.commands.CommandManager;
import com.shade.platform.ui.commands.CommandManagerChangeListener;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.editors.SaveableEditor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UncheckedIOException;

public class PropertyEditor extends JSplitPane implements SaveableEditor {
    private final NavigatorEditorInput input;
    private final Tree tree;
    private final CommandManager commandManager;

    private ValueViewer activeValueViewer;

    public PropertyEditor(@NotNull NavigatorEditorInput input) {
        final PropertyRootNode root;

        try {
            root = new PropertyRootNode(CoreBinary.from(
                input.getNode().getPackfile().extract(input.getNode().getHash()),
                input.getProject().getTypeRegistry(),
                true
            ));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.input = input;
        this.tree = new Tree(root);
        this.tree.setModel(new PropertyTreeModel(tree, root));
        this.tree.setCellRenderer(new PropertyTreeCellRenderer(tree.getModel()));
        this.tree.setCellEditor(new PropertyTreeCellEditor(this));
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
            Application.getMenuService().createContextMenu(tree, MenuConstants.CTX_MENU_PROPERTY_EDITOR_ID, context)
        );
        Application.getMenuService().createContextMenuKeyBindings(
            tree,
            MenuConstants.CTX_MENU_PROPERTY_EDITOR_ID,
            context
        );

        final JScrollPane propertiesTreePane = new JScrollPane(tree);
        propertiesTreePane.setBorder(null);

        setLeftComponent(propertiesTreePane);
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
    public NavigatorEditorInput getInput() {
        return input;
    }

    @Nullable
    public RTTIType<?> getSelectedType() {
        if (tree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    public Object getSelectedValue() {
        if (tree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getObject();
        }
        return null;
    }

    public void setSelectedValue(@Nullable Object value) {
        if (value instanceof RTTIObject object && object.getType().isInstanceOf("GGUUID")) {
            tree.getModel()
                .findChild(new VoidProgressMonitor(), child -> {
                    if (child instanceof PropertyObjectNode pon && pon.getObject() instanceof RTTIObject obj) {
                        return obj.getType().isInstanceOf("RTTIRefObject") && object.equals(obj.get("ObjectUUID"));
                    } else {
                        return false;
                    }
                })
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
                case "editor" -> PropertyEditor.this;
                case "selection" -> tree.getLastSelectedPathComponent();
                default -> null;
            };
        }
    }
}
