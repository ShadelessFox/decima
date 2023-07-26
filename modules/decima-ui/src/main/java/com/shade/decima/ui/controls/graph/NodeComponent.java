package com.shade.decima.ui.controls.graph;

import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.platform.ui.controls.ColoredComponent;
import com.shade.platform.ui.controls.TextAttributes;
import com.shade.util.NotNull;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class NodeComponent extends JComponent {
    private final RTTIObject object;

    public NodeComponent(@NotNull RTTIObject object) {
        this.object = object;

        setLayout(new MigLayout("wrap"));
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        setOpaque(true);

        final Handler handler = new Handler();
        addMouseListener(handler);
        addMouseMotionListener(handler);

        final ColoredComponent title = new ColoredComponent();
        title.append(object.type().getFullTypeName(), TextAttributes.REGULAR_BOLD_ATTRIBUTES);
        title.setOpaque(false);

        final ColoredComponent description = new ColoredComponent();
        description.append(RTTIUtils.uuidToString(object.obj("ObjectUUID")), TextAttributes.GRAYED_ATTRIBUTES);
        description.setOpaque(false);

        add(title);
        add(description);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();

        try {
            doPaint(g2);
        } finally {
            g2.dispose();
        }
    }

    @NotNull
    public RTTIObject getObject() {
        return object;
    }

    private void doPaint(@NotNull Graphics2D g) {
        final GraphComponent graph = (GraphComponent) getParent();
        final boolean selected = graph.isSelected(object);

        g.setColor(UIManager.getColor(selected ? "Graph.nodeSelectionBackground" : "Graph.nodeBackground"));
        g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);

        g.setColor(UIManager.getColor(selected ? "Graph.nodeBorderSelectionColor" : "Graph.nodeBorderColor"));
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    private class Handler extends MouseAdapter {
        private Point origin;
        private boolean dragged;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                origin = e.getLocationOnScreen();

                final GraphComponent graph = (GraphComponent) getParent();

                if (!e.isControlDown() && !graph.isSelected(object)) {
                    graph.clearSelection();
                    graph.addSelection(object);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            final GraphComponent graph = (GraphComponent) getParent();
            final Set<RTTIObject> selection = graph.getSelection();

            if (!dragged) {
                if (!e.isControlDown()) {
                    graph.clearSelection();
                }

                if (graph.isSelected(object) && selection.size() > 1) {
                    graph.removeSelection(object);
                } else {
                    graph.addSelection(object);
                }
            } else {
                final Point shift = new Point();

                for (RTTIObject object : selection) {
                    final NodeComponent component = graph.getNodeComponent(object);
                    shift.x = Math.min(shift.x, component.getX());
                    shift.y = Math.min(shift.y, component.getY());
                }

                if (shift.x < 0 || shift.y < 0) {
                    for (RTTIObject object : selection) {
                        final NodeComponent component = graph.getNodeComponent(object);
                        final Point location = component.getLocation();
                        component.setLocation(location.x - shift.x, location.y - shift.y);
                    }
                }
            }

            origin = null;
            dragged = false;

            graph.revalidate();
            graph.repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (origin == null) {
                return;
            }

            final GraphComponent graph = (GraphComponent) getParent();

            if (!graph.isSelected(object)) {
                if (!e.isControlDown()) {
                    graph.clearSelection();
                }

                graph.addSelection(object);
            }

            final Point current = e.getLocationOnScreen();

            for (RTTIObject object : graph.getSelection()) {
                final NodeComponent comp = graph.getNodeComponent(object);
                comp.setLocation(comp.getX() + current.x - origin.x, comp.getY() + current.y - origin.y);
            }

            graph.repaint();
            origin = current;
            dragged = true;
        }
    }
}
