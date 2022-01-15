package com.shade.decima.ui.editors;

import com.shade.decima.Project;
import com.shade.decima.archive.Archive;
import com.shade.decima.rtti.RTTIType;
import com.shade.decima.rtti.objects.RTTIObject;
import com.shade.decima.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.handlers.ValueCollectionHandler;
import com.shade.decima.ui.handlers.ValueHandler;
import com.shade.decima.ui.handlers.ValueHandlerProvider;
import com.shade.decima.util.NotNull;
import com.shade.decima.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

public class EditorPane extends JPanel {
    private final Project project;

    public EditorPane(@NotNull Project project, @NotNull Archive.FileEntry file) {
        this.project = project;

        final DefaultMutableTreeNode root = createNodeFromFile(file);
        final JTree properties = new JTree(new DefaultTreeModel(root));
        properties.setCellRenderer(new StyledListCellRenderer());
        properties.expandPath(new TreePath(root.getPath()));

        setLayout(new MigLayout("ins 0,fill", "[grow,fill]", "[grow,fill]"));

        final JScrollPane panel = new JScrollPane(properties);
        panel.setBorder(null);
        add(panel);
    }

    @NotNull
    private DefaultMutableTreeNode createNodeFromFile(@NotNull Archive.FileEntry file) {
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
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        final StringBuilder sb = new StringBuilder("<html>");
        final String inline = handler.getInlineValue(type, value);

        if (name != null) {
            sb.append("<font color=#7f0000>%s</font> = ".formatted(escapeLabelName(name)));
        }

        if (inline != null) {
            sb.append(inline);
        } else {
            sb.append("<font color=gray>{%s}</font>".formatted(escapeLabelName(RTTITypeRegistry.getFullTypeName(type))));
        }

        if (handler instanceof ValueCollectionHandler) {
            final ValueCollectionHandler<Object, Object> container = (ValueCollectionHandler<Object, Object>) handler;
            final Collection<?> children = container.getChildren(type, value);

            if (type.getKind() == RTTIType.Kind.CONTAINER) {
                sb.append(" size = ").append(children.size());
            }

            for (Object child : children) {
                append(
                    node,
                    container.getChildName(type, value, child),
                    container.getChildType(type, value, child),
                    container.getChildValue(type, value, child)
                );
            }
        }

        sb.append("</html>");

        node.setUserObject(sb.toString());

        root.add(node);
    }

    @NotNull
    private static String escapeLabelName(@NotNull String label) {
        return label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @NotNull
    private static String unescapeLabelName(@NotNull String label) {
        return label.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static class StyledListCellRenderer extends DefaultTreeCellRenderer {
        private static final Pattern TAG_PATTERN = Pattern.compile("<.*?>");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value != null && selected) {
                value = unescapeLabelName(TAG_PATTERN.matcher(value.toString()).replaceAll(""));
            }

            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }
}
