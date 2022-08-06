package com.shade.decima.ui.editor.property;

import com.shade.decima.model.app.DataContext;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.editor.Editor;
import com.shade.decima.ui.editor.EditorController;
import com.shade.decima.ui.editor.EditorInput;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;

public class PropertyEditorPane extends JSplitPane implements Editor, EditorController {
    private final EditorInput input;

    private final Project project;
    private final Packfile packfile;

    private final NavigatorTree propertiesTree;
    private final JScrollPane viewerPane;
    private final JLabel viewerPanePlaceholder;

    private ValueViewer activeValueViewer;

    public PropertyEditorPane(@NotNull EditorInput input) {
        final NavigatorFileNode node = input.getNode();

        this.input = input;
        this.project = UIUtils.getProject(node);
        this.packfile = UIUtils.getPackfile(node);

        final NavigatorNode root = createNodeFromFile(node.getHash());

        propertiesTree = new NavigatorTree(root);
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

        viewerPane = new JScrollPane();
        viewerPanePlaceholder = UIUtils.Labels.h1("No preview available");
        viewerPanePlaceholder.setHorizontalAlignment(SwingConstants.CENTER);

        setLeftComponent(new JScrollPane(propertiesTree));
        setRightComponent(viewerPane);
        setResizeWeight(0.75);
        setOneTouchExpandable(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                UIUtils.minimizePanel(PropertyEditorPane.this, false);
                removeComponentListener(this);
            }
        });

        updateCurrentViewer();
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return this;
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @NotNull
    @Override
    public EditorController getController() {
        return this;
    }

    @Nullable
    @Override
    public RTTIType<?> getSelectedType() {
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    @Override
    public Object getSelectedValue() {
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getObject();
        }
        return null;
    }

    @Override
    public void setSelectedValue(@Nullable Object value) {
        if (value instanceof RTTIObject object && object.getType().isInstanceOf("GGUUID")) {
            propertiesTree
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

    @NotNull
    @Override
    public JComponent getFocusComponent() {
        return propertiesTree;
    }

    private void updateCurrentViewer() {
        final RTTIType<?> type = getSelectedType();

        if (type != null) {
            final ValueViewer viewer = ValueEditorProvider.findValueViewer(type);

            if (viewer != null) {
                if (activeValueViewer != viewer) {
                    activeValueViewer = viewer;
                    viewerPane.setViewportView(viewer.createComponent());
                }

                activeValueViewer.refresh((JComponent) viewerPane.getViewport().getView(), this);
                return;
            }
        }

        activeValueViewer = null;
        viewerPane.setViewportView(viewerPanePlaceholder);
    }

    @NotNull
    private NavigatorNode createNodeFromFile(long hash) {
        final CoreBinary binary;

        try {
            binary = CoreBinary.from(packfile.extract(hash), project.getTypeRegistry());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new PropertyRootNode(null, binary);
    }

    private class EditorContext implements DataContext {
        @Override
        public Object getData(@NotNull String key) {
            return switch (key) {
                case "editor" -> PropertyEditorPane.this;
                case "selection" -> getSelectedValue();
                default -> null;
            };
        }
    }
}
