package com.shade.decima.ui.controls.graph;

import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.util.graph.Graph;
import com.shade.decima.model.util.graph.GraphLayout;
import com.shade.decima.model.util.graph.GraphLayoutConfig;
import com.shade.decima.model.util.graph.impl.HorizontalGraphVisualizer;
import com.shade.util.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

public class GraphComponent extends JComponent {
    private final Graph<RTTIObject> graph;
    private final Map<RTTIObject, NodeComponent> components = new HashMap<>();
    private final Set<RTTIObject> selection = new HashSet<>();
    private final List<GraphSelectionListener> listeners = new ArrayList<>();

    private Rectangle pendingSelection;
    private Insets padding = new Insets(20, 20, 20, 20);
    private int horizontalGap = 50;
    private int verticalGap = 10;

    public GraphComponent(@NotNull Graph<RTTIObject> graph, @NotNull Set<RTTIObject> selection) {
        this.graph = graph;

        setLayout(null);
        setOpaque(true);

        final Handler handler = new Handler();
        addMouseListener(handler);
        addMouseMotionListener(handler);

        for (RTTIObject object : graph.vertexSet()) {
            final NodeComponent component = new NodeComponent(this, object);
            components.put(object, component);
            add(component);
        }

        for (RTTIObject object : selection) {
            addSelection(object);
        }

        layoutGraph();
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            super.paintComponent(g);
            doPaintBack(g2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            super.paintChildren(g);
            doPaintFront(g2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }

        final Dimension dimension = new Dimension();

        for (int i = 0; i < getComponentCount(); i++) {
            final Component component = getComponent(i);
            dimension.width = Math.max(dimension.width, component.getX() + component.getWidth());
            dimension.height = Math.max(dimension.height, component.getY() + component.getHeight());
        }

        return dimension;
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }

        return getPreferredSize();
    }

    @NotNull
    public Insets getPadding() {
        return padding;
    }

    public void setPadding(@NotNull Insets padding) {
        if (this.padding.equals(padding)) {
            return;
        }
        this.padding = padding;
        layoutGraph();
    }

    public int getHorizontalGap() {
        return horizontalGap;
    }

    public void setHorizontalGap(int horizontalGap) {
        if (this.horizontalGap == horizontalGap) {
            return;
        }
        this.horizontalGap = horizontalGap;
        layoutGraph();
    }

    public int getVerticalGap() {
        return verticalGap;
    }

    public void setVerticalGap(int verticalGap) {
        if (this.verticalGap == verticalGap) {
            return;
        }
        this.verticalGap = verticalGap;
        layoutGraph();
    }

    @NotNull
    public NodeComponent getNodeComponent(@NotNull RTTIObject object) {
        return components.get(object);
    }

    @NotNull
    public Set<RTTIObject> getSelection() {
        return selection;
    }

    public boolean isSelected(@NotNull RTTIObject object) {
        return selection.contains(object);
    }

    public void addSelection(@NotNull RTTIObject object) {
        if (selection.add(object)) {
            setComponentZOrder(components.get(object), 0);
            repaint();
        }
    }

    public void removeSelection(@NotNull RTTIObject object) {
        if (selection.remove(object)) {
            repaint();
        }
    }

    public void clearSelection() {
        selection.clear();
        repaint();
    }

    public void addSelectionListener(@NotNull GraphSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(@NotNull GraphSelectionListener listener) {
        listeners.add(listener);
    }

    protected void fireSelectionEvent(@NotNull BiConsumer<GraphSelectionListener, RTTIObject> consumer, @NotNull RTTIObject object) {
        for (GraphSelectionListener listener : listeners) {
            consumer.accept(listener, object);
        }
    }

    private void doPaintBack(@NotNull Graphics2D g) {
        doPaintBackground(g);

        for (RTTIObject source : graph.vertexSet()) {
            final NodeComponent sourceComponent = components.get(source);

            for (RTTIObject target : graph.outgoingVerticesOf(source)) {
                final NodeComponent targetComponent = components.get(target);

                doPaintEdge(g, sourceComponent, targetComponent);
            }
        }
    }

    private void doPaintEdge(@NotNull Graphics2D g, @NotNull NodeComponent source, @NotNull NodeComponent target) {
        final int sx = source.getX() + source.getWidth();
        final int sy = source.getY() + source.getHeight() / 2;
        final int tx = target.getX();
        final int ty = target.getY() + target.getHeight() / 2;

        final Path2D path = new Path2D.Float();
        path.moveTo(sx, sy);
        path.curveTo(
            Math.max(tx, sx) + (sx - tx) / 2f, sy,
            Math.min(tx, sx) - (sx - tx) / 2f, ty,
            tx, ty
        );

        final boolean sourceSelected = isSelected(source.getObject());
        final boolean targetSelected = isSelected(target.getObject());

        if (sourceSelected != targetSelected) {
            g.setPaint(new GradientPaint(
                sx, sy, UIManager.getColor(sourceSelected ? "Graph.edgeSelectionBackground" : "Graph.edgeBackground"),
                tx, ty, UIManager.getColor(targetSelected ? "Graph.edgeSelectionBackground" : "Graph.edgeBackground")
            ));
        } else if (sourceSelected) {
            g.setPaint(UIManager.getColor("Graph.edgeSelectionBackground"));
        } else {
            g.setPaint(UIManager.getColor("Graph.edgeBackground"));
        }

        g.draw(path);
    }

    private void doPaintBackground(@NotNull Graphics2D g) {
        g.setColor(UIManager.getColor("Graph.viewportBackground"));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(UIManager.getColor("Graph.viewportGridColor"));

        for (int y = 10; y < getHeight(); y += 20) {
            for (int x = 10; x < getWidth(); x += 20) {
                g.drawLine(x - 1, y, x + 1, y);
                g.drawLine(x, y - 1, x, y + 1);
            }
        }
    }

    private void doPaintFront(@NotNull Graphics2D g) {
        final Rectangle selection = pendingSelection;

        if (selection != null) {
            g.setColor(UIManager.getColor("Graph.nodeBorderSelectionColor"));
            g.drawRect(selection.x, selection.y, selection.width - 1, selection.height - 1);

            g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
            g.fillRect(selection.x + 1, selection.y + 1, selection.width - 2, selection.height - 2);
        }
    }

    private void layoutGraph() {
        final var visualizer = new HorizontalGraphVisualizer<RTTIObject>();
        final var layouts = visualizer.create(graph, new GraphLayoutConfig<>() {
            @NotNull
            @Override
            public Dimension getSize(@NotNull RTTIObject vertex) {
                return components.get(vertex).getPreferredSize();
            }

            @NotNull
            @Override
            public Dimension getSpacing() {
                return new Dimension(horizontalGap, verticalGap);
            }
        });

        for (GraphLayout<RTTIObject> layout : layouts) {
            final var component = components.get(layout.getVertex());
            final var location = layout.getLocation();
            component.setLocation(location.x + padding.left, location.y + padding.top);
            component.setSize(layout.getSize());
        }
    }

    private class Handler extends MouseAdapter {
        private Point origin;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                origin = e.getPoint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (origin == null) {
                return;
            }

            if (!e.isControlDown()) {
                clearSelection();
            }

            if (pendingSelection != null) {
                for (int i = 0; i < getComponentCount(); i++) {
                    final Component c = getComponent(i);

                    if (c instanceof NodeComponent node && pendingSelection.contains(c.getX(), c.getY(), c.getWidth(), c.getHeight())) {
                        addSelection(node.getObject());
                    }
                }
            }

            origin = null;
            pendingSelection = null;
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin == null) {
                return;
            }

            final Point current = e.getPoint();
            final int x = Math.min(origin.x, current.x);
            final int y = Math.min(origin.y, current.y);
            final int w = Math.max(origin.x, current.x) - x;
            final int h = Math.max(origin.y, current.y) - y;

            pendingSelection = new Rectangle(x, y, w, h);
            repaint();
        }
    }
}
