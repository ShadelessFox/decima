package com.shade.decima.ui.editor;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.runtime.VoidProgressMonitor;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.UIUtils;
import com.shade.decima.ui.action.Actions;
import com.shade.decima.ui.data.ValueEditorProvider;
import com.shade.decima.ui.data.ValueViewer;
import com.shade.decima.ui.navigator.NavigatorNode;
import com.shade.decima.ui.navigator.NavigatorTree;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class PropertyEditorPane extends JSplitPane implements EditorController {
    private final Project project;
    private final Packfile packfile;
    private final NavigatorFileNode node;

    private final NavigatorTree propertiesTree;
    private final JScrollPane viewerPane;
    private final JComponent viewerPanePlaceholder;

    private ValueViewer activeValueViewer;

    public PropertyEditorPane(@NotNull NavigatorFileNode node) {
        this.project = UIUtils.getProject(node);
        this.packfile = UIUtils.getPackfile(node);
        this.node = node;

        final NavigatorNode root = createNodeFromFile(node.getHash());

        propertiesTree = new NavigatorTree(root);
        propertiesTree.getTree().setCellRenderer(new PropertyTreeCellRenderer());
        propertiesTree.getTree().setSelectionPath(new TreePath(root));
        propertiesTree.getTree().addTreeSelectionListener(e -> updateCurrentViewer());
        propertiesTree.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final JTree tree = propertiesTree.getTree();
                if (SwingUtilities.isRightMouseButton(e)) {
                    final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        tree.setSelectionPath(path);
                        final JPopupMenu menu = new JPopupMenu();
                        Actions.contribute(menu, "popup:properties");
                        menu.show(tree, e.getX(), e.getY());
                    }
                }
            }
        });
        propertiesTree.setBorder(null);

        viewerPane = new JScrollPane();
        viewerPanePlaceholder = new JLabel("No preview available", SwingConstants.CENTER);
        viewerPanePlaceholder.setFont(viewerPanePlaceholder.getFont().deriveFont(24.0f));

        setLeftComponent(propertiesTree);
        setRightComponent(viewerPane);
        setResizeWeight(0.75);
        setOneTouchExpandable(true);

        updateCurrentViewer();
    }

    public void setSelectedObject(@NotNull RTTIObject uuid) {
        try {
            final NavigatorNode node = propertiesTree.findChild(new VoidProgressMonitor(), child -> {
                if (child instanceof PropertyObjectNode pon && pon.getObject() instanceof RTTIObject obj) {
                    return obj.getType().isInstanceOf("RTTIRefObject") && uuid.equals(obj.get("ObjectUUID"));
                } else {
                    return false;
                }
            });

            if (node != null) {
                final TreePath path = new TreePath(propertiesTree.getModel().getPathToRoot(node));
                propertiesTree.getTree().setSelectionPath(path);
                propertiesTree.getTree().scrollPathToVisible(path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public RTTIType<?> getSelectedType() {
        if (propertiesTree.getTree().getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getType();
        }
        return null;
    }

    @Nullable
    @Override
    public Object getSelectedValue() {
        if (propertiesTree.getTree().getLastSelectedPathComponent() instanceof PropertyObjectNode node) {
            return node.getObject();
        }
        return null;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
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
        return propertiesTree.getTree();
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
    private NavigatorNode createNodeFromFile(long hash) {
        final CoreBinary binary;

        try {
            binary = CoreBinary.from(packfile.extract(hash), project.getTypeRegistry());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new PropertyRootNode(null, binary);
    }
}
