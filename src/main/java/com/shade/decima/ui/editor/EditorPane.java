package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreObject;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.handler.ValueCollectionHandler;
import com.shade.decima.ui.handler.ValueHandler;
import com.shade.decima.ui.handler.ValueHandlerProvider;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;
import java.util.Collection;

public class EditorPane extends JSplitPane implements EditorController {
    private final Project project;
    private final Packfile packfile;
    private final NavigatorFileNode node;

    private final JTree propertiesTree;
    private final JScrollPane viewerPane;
    private final JComponent viewerPanePlaceholder;

    private ValueViewer activeValueViewer;

    public EditorPane(@NotNull NavigatorFileNode node) {
        this.project = UIUtils.getProject(node);
        this.packfile = UIUtils.getPackfile(node);
        this.node = node;

        final DefaultMutableTreeNode root = createNodeFromFile(node.getHash());
        propertiesTree = new JTree(new DefaultTreeModel(root));
        propertiesTree.setCellRenderer(new PropertyTreeCellRenderer());
        propertiesTree.setCellEditor(new DefaultTreeCellEditor(propertiesTree, (DefaultTreeCellRenderer) propertiesTree.getCellRenderer(), new PropertyTreeCellEditor()));
        propertiesTree.setEditable(true);
        propertiesTree.setSelectionRow(0);
        propertiesTree.addTreeSelectionListener(e -> updateCurrentViewer());
        propertiesTree.expandPath(new TreePath(root.getPath()));

        final JScrollPane propertiesPanel = new JScrollPane(propertiesTree);
        propertiesPanel.setBorder(null);

        viewerPane = new JScrollPane();

        viewerPanePlaceholder = new JLabel("No preview available", SwingConstants.CENTER);
        viewerPanePlaceholder.setFont(viewerPanePlaceholder.getFont().deriveFont(24.0f));

        setLeftComponent(propertiesPanel);
        setRightComponent(viewerPane);
        setResizeWeight(0.75);
        setOneTouchExpandable(true);

        updateCurrentViewer();
    }

    @NotNull
    public Packfile getPackfile() {
        return packfile;
    }

    @NotNull
    public NavigatorFileNode getNode() {
        return node;
    }

    @NotNull
    public JTree getPropertiesTree() {
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
                    viewerPane.setBorder(null);
                }

                activeValueViewer.refresh((JComponent) viewerPane.getViewport().getView(), this);
                return;
            }
        }

        activeValueViewer = null;
        viewerPane.setViewportView(viewerPanePlaceholder);
        viewerPane.setBorder(null);
    }

    @NotNull
    private DefaultMutableTreeNode createNodeFromFile(long hash) {
        // TODO: Can we create nodes dynamically rather than prefilling it here?
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("<html><font color=gray>&lt;root&gt;</font></html>", true);
        final CoreObject coreObject;

        try {
            coreObject = CoreObject.from(packfile.extract(hash), project.getTypeRegistry());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (RTTIObject object : coreObject.getEntries()) {
            append(root, object.getType(), object);
        }

        return root;
    }

    private void append(@NotNull DefaultMutableTreeNode root, @NotNull RTTIType<?> type, @NotNull Object value) {
        append(root, RTTITypeRegistry.getFullTypeName(type), type, value);
    }

    @SuppressWarnings("unchecked")
    private void append(@NotNull DefaultMutableTreeNode root, @Nullable String name, @NotNull RTTIType<?> type, @NotNull Object value) {
        final ValueHandler handler = ValueHandlerProvider.getValueHandler(type);
        final PropertyTreeNode node = new PropertyTreeNode(type, handler, name, value);

        if (handler instanceof ValueCollectionHandler) {
            final ValueCollectionHandler<Object, Object> container = (ValueCollectionHandler<Object, Object>) handler;
            final Collection<?> children = container.getChildren(type, value);

            for (Object child : children) {
                append(
                    node,
                    container.getChildName(type, value, child),
                    container.getChildType(type, value, child),
                    container.getChildValue(type, value, child)
                );
            }
        }

        root.add(node);
    }

    @Nullable
    @Override
    public RTTIType<?> getSelectedType() {
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyTreeNode node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    @Override
    public Object getSelectedValue() {
        if (propertiesTree.getLastSelectedPathComponent() instanceof PropertyTreeNode node) {
            return node.getUserObject();
        }
        return null;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
