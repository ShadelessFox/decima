package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.util.Graph;
import com.shade.decima.ui.controls.graph.GraphComponent;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.shade.decima.ui.menu.MenuConstants.*;

@MenuItemRegistration(parent = CTX_MENU_CORE_EDITOR_ID, name = "Show &Graph", keystroke = "ctrl alt U", group = CTX_MENU_CORE_EDITOR_GROUP_GENERAL, order = 3000)
public class ShowGraphItem extends MenuItem {
    @Override
    public void perform(@NotNull MenuItemContext ctx) {
        final CoreEditor editor = (CoreEditor) ctx.getData(PlatformDataKeys.EDITOR_KEY);
        final CoreBinary binary = editor.getBinary();
        final ValueController<RTTIObject> controller = editor.getValueController();

        final Graph<RTTIObject> graph = new Graph<>();

        for (RTTIObject entry : binary.entries()) {
            graph.addVertex(entry);
            buildGraph(binary, entry, entry, graph);
        }

        final String name;
        final Set<RTTIObject> roots;

        if (controller != null) {
            removeUnreachableVertices(controller.getValue(), graph);
            name = controller.getValueLabel();
            roots = Set.of(controller.getValue());
        } else {
            name = editor.getInput().getName();
            roots = graph.vertexSet().stream()
                .filter(vertex -> graph.incomingVerticesOf(vertex).isEmpty())
                .collect(Collectors.toSet());
        }

        new GraphDialog(graph, roots, name).showDialog(JOptionPane.getRootFrame());
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);
        return selection instanceof CoreNodeBinary
            || selection instanceof CoreNodeObject node && node.getType() instanceof RTTIClass cls && cls.isInstanceOf("RTTIRefObject");
    }

    private static void removeUnreachableVertices(@NotNull RTTIObject root, @NotNull Graph<RTTIObject> graph) {
        final Set<RTTIObject> unreachableVertices = new HashSet<>(graph.vertexSet());
        final Deque<RTTIObject> outgoingQueue = new ArrayDeque<>(List.of(root));
        final Deque<RTTIObject> incomingQueue = new ArrayDeque<>(List.of(root));

        while (!incomingQueue.isEmpty()) {
            final RTTIObject vertex = incomingQueue.remove();

            for (RTTIObject v : graph.incomingVerticesOf(vertex)) {
                incomingQueue.offer(v);
            }

            unreachableVertices.remove(vertex);
        }

        while (!outgoingQueue.isEmpty()) {
            final RTTIObject vertex = outgoingQueue.remove();

            for (RTTIObject v : graph.outgoingVerticesOf(vertex)) {
                outgoingQueue.offer(v);
            }

            unreachableVertices.remove(vertex);
        }

        for (RTTIObject vertex : unreachableVertices) {
            graph.removeVertex(vertex);
        }
    }

    private static void buildGraph(@NotNull CoreBinary binary, @NotNull RTTIObject source, @NotNull Object object, @NotNull Graph<RTTIObject> graph) {
        if (object instanceof RTTIObject obj) {
            for (RTTIClass.Field<?> field : obj.type().getFields()) {
                buildGraph(binary, source, field.get(obj), graph);
            }
        } else if (object instanceof Object[] arr) {
            for (Object element : arr) {
                buildGraph(binary, source, element, graph);
            }
        } else if (object instanceof RTTIReference.Internal ref) {
            final RTTIObject target = binary.find(ref.uuid());
            if (target != null) {
                graph.addVertex(target);
                graph.addEdge(source, target);
            }
        }
    }

    private static class GraphDialog extends BaseDialog {
        private final Graph<RTTIObject> graph;
        private final Set<RTTIObject> selection;

        public GraphDialog(@NotNull Graph<RTTIObject> graph, @NotNull Set<RTTIObject> selection, @NotNull String name) {
            super("Dependency graph for '%s'".formatted(name));
            this.graph = graph;
            this.selection = selection;
        }

        @NotNull
        @Override
        protected JComponent createContentsPane() {
            final GraphComponent view = new GraphComponent(graph, selection);
            final Insets padding = view.getPadding();
            final Dimension size = view.getPreferredSize();

            // 2 extra pixels for the border
            size.width = Math.min(1280, size.width + padding.right + 2);
            size.height = Math.min(720, size.height + padding.bottom + 2);

            final JScrollPane pane = new JScrollPane(view);
            pane.setPreferredSize(size);
            // pane.setBorder(null);

            return pane;
        }

        @NotNull
        @Override
        protected ButtonDescriptor[] getButtons() {
            return new ButtonDescriptor[]{BUTTON_OK};
        }
    }
}
