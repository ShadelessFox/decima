package com.shade.decima.hfw.ui.editor;

import com.shade.decima.hfw.ObjectStreamingSystem;
import com.shade.decima.hfw.StreamingGraphResource;
import com.shade.decima.hfw.StreamingObjectReader;
import com.shade.decima.hfw.archive.StorageReadDevice;
import com.shade.decima.hfw.ui.editor.tree.TreeStructure;
import com.shade.decima.hfw.ui.editor.tree.TreeStructureModel;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTICoreFileReader.LoggingErrorHandlingStrategy;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.ui.editor.FileEditorInput;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreEditorInput;
import com.shade.platform.ui.controls.ColoredTreeCellRenderer;
import com.shade.platform.ui.controls.CommonTextAttributes;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.platform.ui.editors.Editor;
import com.shade.platform.ui.editors.EditorInput;
import com.shade.platform.ui.editors.EditorManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class GraphEditor implements Editor {
    private final FileEditorInput input;
    private final StreamingGraphResource streamingGraph;
    private final ObjectStreamingSystem streamingSystem;

    private JTree tree;

    public GraphEditor(@NotNull FileEditorInput input) throws IOException {
        this.input = input;

        try (InputStream is = new BufferedInputStream(Files.newInputStream(input.getPath()))) {
            final RTTICoreFile file = input.getProject().getCoreFileReader().read(is, LoggingErrorHandlingStrategy.getInstance());
            streamingGraph = new StreamingGraphResource(file.objects().get(0), input.getProject().getRTTIFactory());
        }

        final StorageReadDevice device = new StorageReadDevice(input.getProject().getContainer());
        for (String file : streamingGraph.getFiles()) {
            device.mount(file);
        }

        streamingSystem = new ObjectStreamingSystem(device, streamingGraph);
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        tree = new JTree();
        tree.setModel(new TreeStructureModel<>((TreeStructure) new GraphTreeStructure()));
        tree.setCellRenderer(new GraphTreeCellRenderer().withTags(tree));

        return UIUtils.createBorderlessScrollPane(tree);
    }

    @NotNull
    @Override
    public EditorInput getInput() {
        return input;
    }

    @Override
    public void setFocus() {
        tree.requestFocusInWindow();
    }

    @Override
    public boolean isFocused() {
        return tree.isFocusOwner();
    }

    private void openGroup(@NotNull RTTIObject group, @NotNull RTTIObject uuid) throws IOException {
        System.out.println("Open group: " + group.i32("GroupID") + ", object: " + RTTIUtils.uuidToString(uuid));
        final StreamingObjectReader reader = new StreamingObjectReader(input.getProject().getRTTIFactory(), streamingSystem);

        final StreamingObjectReader.ObjectResult result = reader.readObject(uuid);
        final RTTICoreFile coreFile = new CoreBinary(List.of(result.groupResult().root().objects()));

        final Editor editor = EditorManager.getInstance().openEditor(new CoreEditorInput(coreFile, "Group: " + group.i32("GroupID"), input.getProject()), true);
        if (editor instanceof CoreEditor e) {
            e.setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(RTTIUtils.uuidToString(uuid))));
        }
    }

    private class GraphTreeCellRenderer extends ColoredTreeCellRenderer<Object> {
        @Override
        protected void customizeCellRenderer(@NotNull JTree tree, @NotNull Object value, boolean selected, boolean expanded, boolean focused, boolean leaf, int row) {
            if (value instanceof GraphGroupNode node) {
                append("[", TextAttributes.REGULAR_ATTRIBUTES);
                append(String.valueOf(node.group().i32("GroupID")), CommonTextAttributes.IDENTIFIER_ATTRIBUTES);
                append("] ", TextAttributes.REGULAR_ATTRIBUTES);
            }

            if (value instanceof StreamingGraphNode) {
                append("StreamingGraphResource", TextAttributes.REGULAR_ATTRIBUTES);
            } else if (value instanceof GroupedByTypeNode node) {
                append(node.type.getTypeName(), TextAttributes.REGULAR_ATTRIBUTES);
                append(" (" + node.children.size() + ")", TextAttributes.GRAYED_ATTRIBUTES);
            } else if (value instanceof RootObjectNode node) {
                append("{", TextAttributes.REGULAR_ATTRIBUTES);
                append(RTTIUtils.uuidToString(node.uuid()), TextAttributes.REGULAR_ATTRIBUTES);
                append("}", TextAttributes.REGULAR_ATTRIBUTES);
                append(" (" + node.type + ")", TextAttributes.GRAYED_ATTRIBUTES);
                append(" open", TextAttributes.LINK_ATTRIBUTES, e -> {
                    try {
                        openGroup(node.group, node.uuid);
                    } catch (IOException ex) {
                        UIUtils.showErrorDialog(ex, "Error opening group");
                    }
                });
            } else if (value instanceof StreamingGroupNode) {
                append("StreamingGroupData", TextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }

    private class GraphTreeStructure implements TreeStructure {
        @NotNull
        @Override
        public Object getRoot() {
            return new StreamingGraphNode();
        }

        @NotNull
        @Override
        public Object[] getChildren(@NotNull Object element) {
            if (element instanceof StreamingGraphNode) {
                final RTTIObject[] uuids = streamingGraph.getRootUUIDs();
                final Map<RTTIClass, List<RootObjectNode>> groups = new HashMap<>();

                for (RTTIObject uuid : uuids) {
                    final RTTIObject group = Objects.requireNonNull(streamingGraph.getGroup(uuid), () -> "Group not found: " + RTTIUtils.uuidToString(uuid));
                    final int index = Objects.requireNonNull(streamingGraph.getRootIndex(uuid), () -> "Root object not found for group: " + RTTIUtils.uuidToString(uuid));
                    final RTTIClass type = streamingGraph.getType(group.i32("TypeStart") + index);
                    groups.computeIfAbsent(type, t -> new ArrayList<>()).add(new RootObjectNode(uuid, group, type));
                }

                return groups.entrySet().stream()
                    .map(e -> new GroupedByTypeNode(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparing(e -> e.type.getTypeName()))
                    .toArray();
            }

            if (element instanceof GroupedByTypeNode node) {
                return node.children.toArray();
            }

            if (element instanceof GraphGroupNode node) {
                final var children = new StreamingGroupNode[node.group().i32("SubGroupCount")];
                final var subgroups = streamingGraph.getSubGroups();
                for (int i = 0; i < children.length; i++) {
                    children[i] = new StreamingGroupNode(Objects.requireNonNull(streamingGraph.getGroup(subgroups[node.group().i32("SubGroupStart") + i])));
                }
                return children;
            }

            throw new IllegalArgumentException();
        }

        @Override
        public boolean hasChildren(@NotNull Object element) {
            return element instanceof StreamingGraphNode
                || element instanceof GroupedByTypeNode tn && !tn.children.isEmpty()
                || element instanceof GraphGroupNode gn && gn.group().i32("SubGroupCount") > 0;
        }
    }

    private record StreamingGraphNode() {
    }

    private sealed interface GraphGroupNode {
        @NotNull
        RTTIObject group();
    }

    private record GroupedByTypeNode(@NotNull RTTIClass type, @NotNull List<RootObjectNode> children) {}

    private record RootObjectNode(@NotNull RTTIObject uuid, @NotNull RTTIObject group, @NotNull RTTIClass type) implements GraphGroupNode {}

    private record StreamingGroupNode(@NotNull RTTIObject group) implements GraphGroupNode {}
}
