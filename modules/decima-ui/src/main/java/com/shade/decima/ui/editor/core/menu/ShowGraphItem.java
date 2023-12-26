package com.shade.decima.ui.editor.core.menu;

import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.path.RTTIPath;
import com.shade.decima.model.rtti.path.RTTIPathElement;
import com.shade.decima.model.util.graph.Graph;
import com.shade.decima.model.util.graph.impl.DirectedAcyclicGraph;
import com.shade.decima.ui.controls.graph.GraphComponent;
import com.shade.decima.ui.controls.graph.GraphSelectionListener;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.decima.ui.editor.core.CoreNodeBinary;
import com.shade.decima.ui.editor.core.CoreNodeObject;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.ui.PlatformDataKeys;
import com.shade.platform.ui.dialogs.BaseDialog;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuItem;
import com.shade.platform.ui.menus.MenuItemContext;
import com.shade.platform.ui.menus.MenuItemRegistration;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

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

        ProgressDialog
            .showProgressDialog(null, "Building graph", monitor -> buildGraph(monitor, editor))
            .ifPresent(info -> new GraphDialog(editor, info).showDialog(JOptionPane.getRootFrame()));
    }

    @Override
    public boolean isVisible(@NotNull MenuItemContext ctx) {
        final Object selection = ctx.getData(PlatformDataKeys.SELECTION_KEY);
        return selection instanceof CoreNodeBinary
            || selection instanceof CoreNodeObject node && node.getType() instanceof RTTIClass cls && cls.isInstanceOf("RTTIRefObject");
    }

    @Nullable
    private static GraphInfo buildGraph(@NotNull ProgressMonitor monitor, @NotNull CoreEditor editor) {
        final ValueController<RTTIObject> controller = editor.getValueController();
        final CoreBinary binary = editor.getBinary();
        final Graph<RTTIObject> graph = new DirectedAcyclicGraph<>();

        try (ProgressMonitor.Task task = monitor.begin("Building graph", controller != null ? 2 : 1)) {
            collectVertices(task.split(1), binary, graph);

            if (task.isCanceled()) {
                return null;
            }

            final String name;
            final Set<RTTIObject> roots;

            if (controller != null) {
                removeUnreachableVertices(task.split(1), controller.getValue(), graph);

                name = controller.getValueLabel();
                roots = Set.of(controller.getValue());
            } else {
                name = editor.getInput().getName();
                roots = graph.vertexSet().stream()
                    .filter(vertex -> graph.incomingVerticesOf(vertex).isEmpty())
                    .collect(Collectors.toSet());
            }

            return new GraphInfo(graph, roots, name);
        }
    }

    private static void collectVertices(@NotNull ProgressMonitor monitor, @NotNull CoreBinary binary, @NotNull Graph<RTTIObject> graph) {
        try (ProgressMonitor.Task task = monitor.begin("Collect vertices", binary.entries().size())) {
            for (RTTIObject entry : binary.entries()) {
                if (task.isCanceled()) {
                    break;
                }

                graph.addVertex(entry);
                buildGraph(binary, entry, entry, graph);
                task.worked(1);
            }
        }
    }

    private static void removeUnreachableVertices(@NotNull ProgressMonitor monitor, @NotNull RTTIObject root, @NotNull Graph<RTTIObject> graph) {
        final Set<RTTIObject> unreachableVertices = new HashSet<>(graph.vertexSet());
        final Deque<RTTIObject> outgoingQueue = new ArrayDeque<>(List.of(root));
        final Deque<RTTIObject> incomingQueue = new ArrayDeque<>(List.of(root));

        try (ProgressMonitor.IndeterminateTask task = monitor.begin("Remove unreachable vertices")) {
            while (!incomingQueue.isEmpty() && !task.isCanceled()) {
                final RTTIObject vertex = incomingQueue.remove();

                for (RTTIObject v : graph.incomingVerticesOf(vertex)) {
                    if (task.isCanceled()) {
                        return;
                    }

                    incomingQueue.offer(v);
                }

                unreachableVertices.remove(vertex);
            }

            while (!outgoingQueue.isEmpty() && !task.isCanceled()) {
                final RTTIObject vertex = outgoingQueue.remove();

                for (RTTIObject v : graph.outgoingVerticesOf(vertex)) {
                    if (task.isCanceled()) {
                        return;
                    }

                    outgoingQueue.offer(v);
                }

                unreachableVertices.remove(vertex);
            }

            for (RTTIObject vertex : unreachableVertices) {
                if (task.isCanceled()) {
                    return;
                }

                graph.removeVertex(vertex);
            }
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

    private record GraphInfo(@NotNull Graph<RTTIObject> graph, @NotNull Set<RTTIObject> roots, @NotNull String name) {
    }

    private static class GraphDialog extends BaseDialog {
        private final CoreEditor editor;
        private final Graph<RTTIObject> graph;
        private final Set<RTTIObject> selection;

        public GraphDialog(@NotNull CoreEditor editor, @NotNull GraphInfo info) {
            super("Dependency graph for '%s'".formatted(info.name()));
            this.editor = editor;
            this.graph = info.graph();
            this.selection = info.roots();
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

            view.addSelectionListener(new GraphSelectionListener() {
                @Override
                public void nodeDoubleClicked(@NotNull RTTIObject object) {
                    editor.setSelectionPath(new RTTIPath(new RTTIPathElement.UUID(object)));
                    close();
                }
            });

            return pane;
        }

        @NotNull
        @Override
        protected ButtonDescriptor[] getButtons() {
            return new ButtonDescriptor[]{BUTTON_OK};
        }
    }
}
