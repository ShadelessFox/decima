package com.shade.decima.ui.editor.property;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.NavigatorEditorInput;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import com.shade.platform.model.data.DataContext;
import com.shade.platform.model.runtime.VoidProgressMonitor;
import com.shade.platform.ui.controls.tree.Tree;
import com.shade.platform.ui.controls.tree.TreeNode;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.IOException;

public class PropertyEditor extends JSplitPane implements Editor {
    private final NavigatorEditorInput input;

    private final Project project;
    private final Packfile packfile;
    private final Tree propertiesTree;

    private ValueViewer activeValueViewer;

    public PropertyEditor(@NotNull NavigatorEditorInput input) {
        final NavigatorFileNode node = input.getNode();

        this.input = input;
        this.project = node.getProject();
        this.packfile = node.getPackfile();

        final TreeNode root = createNodeFromFile(node.getHash());

        propertiesTree = new Tree(root);
        propertiesTree.setCellRenderer(new PropertyTreeCellRenderer(propertiesTree.getModel()));
        propertiesTree.setSelectionPath(new TreePath(root));
        propertiesTree.addTreeSelectionListener(e -> updateCurrentViewer());

        final EditorContext context = new EditorContext();
        UIUtils.installPopupMenu(
            propertiesTree,
            Application.getMenuService().createContextMenu(propertiesTree, MenuConstants.CTX_MENU_PROPERTY_EDITOR_ID, context)
        );
        Application.getMenuService().createContextMenuKeyBindings(
            propertiesTree,
            MenuConstants.CTX_MENU_PROPERTY_EDITOR_ID,
            context
        );

        final JScrollPane propertiesTreePane = new JScrollPane(propertiesTree);
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
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    public Object getSelectedValue() {
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getObject();
        }
        return null;
    }

    public void setSelectedValue(@Nullable Object value) {
        if (value instanceof RTTIObject object && object.getType().isInstanceOf("GGUUID")) {
            propertiesTree.getModel()
                .findChild(new VoidProgressMonitor(), child -> {
                    if (child instanceof PropertyObjectNode pon && pon.getObject() instanceof RTTIObject obj) {
                        return obj.getType().isInstanceOf("RTTIRefObject") && object.equals(obj.get("ObjectUUID"));
                    } else {
                        return false;
                    }
                })
                .whenComplete((node, exception) -> {
                    if (exception != null) {
                        throw new RuntimeException(exception);
                    }

                    if (node != null) {
                        final TreePath path = new TreePath(propertiesTree.getModel().getPathToRoot(node));
                        propertiesTree.setSelectionPath(path);
                        propertiesTree.scrollPathToVisible(path);
                    }
                });

        }
    }

    @Override
    public void setFocus() {
        propertiesTree.requestFocusInWindow();
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

    @NotNull
    private TreeNode createNodeFromFile(long hash) {
        final CoreBinary binary;

        try {
            binary = CoreBinary.from(packfile.extract(hash), project.getTypeRegistry(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new PropertyRootNode(null, binary);
    }

    private class EditorContext implements DataContext {
        @Override
        public Object getData(@NotNull String key) {
            return switch (key) {
                case "editor" -> PropertyEditor.this;
                case "selection" -> propertiesTree.getLastSelectedPathComponent();
                default -> null;
            };
        }
    }
}
