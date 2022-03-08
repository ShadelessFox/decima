package com.shade.decima.ui.editors;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.archive.Archive;
import com.shade.decima.model.rtti.RTTIType;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.util.NotNull;
import com.shade.decima.model.util.Nullable;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.ui.handlers.ValueHandler;
import com.shade.decima.ui.handlers.ValueHandlerProvider;
import com.shade.decima.ui.navigator.impl.NavigatorFileNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;
import java.util.Collection;

public class EditorPane extends JPanel {
    private final Project project;
    private final NavigatorFileNode node;

    public EditorPane(@NotNull Project project, @NotNull NavigatorFileNode node) {
        this.project = project;
        this.node = node;

        final DefaultMutableTreeNode root = createNodeFromFile(node.getFile());
        final JTree properties = new JTree(new DefaultTreeModel(root));
        properties.setCellRenderer(new PropertyTreeCellRenderer());
        properties.setCellEditor(new DefaultTreeCellEditor(properties, (DefaultTreeCellRenderer) properties.getCellRenderer(), new PropertyTreeCellEditor()));
        properties.expandPath(new TreePath(root.getPath()));
        properties.setEditable(true);

        setLayout(new MigLayout("ins 0,fill", "[grow,fill]", "[grow,fill]"));

        final JScrollPane panel = new JScrollPane(properties);
        panel.setBorder(null);
        add(panel);
    }

    @NotNull
    public NavigatorFileNode getNode() {
        return node;
    }

    @NotNull
    private DefaultMutableTreeNode createNodeFromFile(@NotNull Archive.FileEntry file) {
        // TODO: Can we create nodes dynamically rather than prefilling it here?
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true);

        try {
            for (RTTIObject object : project.getArchiveManager().readFileObjects(project.getCompressor(), file)) {
                append(root, object.getType(), object);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return root;
    }

    public void append(@NotNull DefaultMutableTreeNode root, @NotNull RTTIType<?> type, @NotNull Object value) {
        append(root, RTTITypeRegistry.getFullTypeName(type), type, value);
    }

    @SuppressWarnings("unchecked")
    public void append(@NotNull DefaultMutableTreeNode root, @Nullable String name, @NotNull RTTIType<?> type, @NotNull Object value) {
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
}
