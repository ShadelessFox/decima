package com.shade.decima.ui.data.viewer.model;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.RenderLoop;
import com.shade.decima.model.viewer.camera.FirstPersonCamera;
import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.isr.Visitor;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.viewer.model.isr.SceneSerializer;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.UIColor;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ModelViewerPanel extends JComponent implements Disposable, PropertyChangeListener {
    public static final DataKey<ModelViewerPanel> PANEL_KEY = new DataKey<>("panel", ModelViewerPanel.class);

    private static final Logger log = LoggerFactory.getLogger(ModelViewerPanel.class);

    private JToolBar topToolbar;
    private JToolBar bottomToolbar;
    private ModelViewport viewport;
    private RenderLoop loop;

    private ValueController<RTTIObject> controller;

    public ModelViewerPanel() {
        bottomToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_MODEL_VIEWER_BOTTOM_ID, key -> switch (key) {
            case "panel" -> this;
            default -> null;
        });

        setLayout(new BorderLayout());
        add(bottomToolbar, BorderLayout.SOUTH);

        try {
            viewport = new ModelViewport(new FirstPersonCamera());
            viewport.setPreferredSize(new Dimension(400, 400));
            viewport.setMinimumSize(new Dimension(100, 100));
            viewport.addPropertyChangeListener(this);
        } catch (Throwable e) {
            log.error("Can't create GL canvas: " + e.getMessage());
        }

        if (viewport != null) {
            final JLabel statusLabel = new JLabel();
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

            topToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_MODEL_VIEWER_ID, key -> switch (key) {
                case "viewport" -> viewport;
                default -> null;
            });
            topToolbar.add(Box.createHorizontalGlue());
            topToolbar.add(statusLabel);

            final JPanel canvasHolder = new JPanel();
            canvasHolder.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIColor.SHADOW));
            canvasHolder.setLayout(new BorderLayout());
            canvasHolder.add(viewport, BorderLayout.CENTER);

            add(topToolbar, BorderLayout.NORTH);
            add(canvasHolder, BorderLayout.CENTER);

            loop = new RenderLoop(JOptionPane.getRootFrame(), viewport) {
                private long renderTime;
                private long updateTime;
                private long framesPassed;

                @Override
                public void beforeRender() {
                    renderTime = System.currentTimeMillis();
                }

                @Override
                public void afterRender() {
                    framesPassed += 1;

                    if (renderTime - updateTime >= 1000) {
                        statusLabel.setText("%.3f ms/frame, %d fps".formatted(1000.0 / framesPassed, framesPassed));
                        updateTime = renderTime;
                        framesPassed = 0;
                    }
                }
            };

            loop.start();
        } else {
            final JLabel placeholder = new JLabel("Preview is not supported");
            placeholder.setHorizontalAlignment(SwingConstants.CENTER);
            placeholder.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

            add(placeholder, BorderLayout.CENTER);
        }

        addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        switch (event.getPropertyName()) {
            case "background", "model", "showOutline" -> MenuManager.getInstance().update(topToolbar);
            case "controller" -> MenuManager.getInstance().update(bottomToolbar);
        }
    }

    @Override
    public void dispose() {
        if (viewport != null) {
            loop.dispose();
            viewport.dispose();
        }
    }

    public void setController(@Nullable ValueController<RTTIObject> controller) {
        if (this.controller != controller) {
            final ValueController<RTTIObject> oldController = this.controller;

            this.controller = controller;

            firePropertyChange("controller", oldController, controller);

            updatePreview();
        }
    }

    private void updatePreview() {
        if (viewport == null) {
            return;
        }

        Node node = null;

        if (controller != null) {
            try {
                node = ProgressDialog
                    .showProgressDialog(null, "Loading model", monitor -> SceneSerializer.serialize(monitor, controller))
                    .orElse(null);
            } catch (Exception e) {
                log.debug("Can't load preview for model of type {}", controller.getValueType(), e);
            }
        }

        if (node != null) {
            node = node.accept(OptimizingVisitor.INSTANCE);
        }

        if (node != null) {
            viewport.setModel(new NodeModel(node, viewport));
        }
    }

    @Nullable
    public ValueController<RTTIObject> getController() {
        return controller;
    }

    private static class OptimizingVisitor implements Visitor {
        private static final OptimizingVisitor INSTANCE = new OptimizingVisitor();

        @Override
        public boolean enterNode(@NotNull Node node) {
            return true;
        }

        @Nullable
        @Override
        public Node leaveNode(@NotNull Node node) {
            if (node.getChildren().isEmpty() && node.getMesh() == null) {
                log.debug("Removing empty node {}", node.getName());
                return null;
            }

            return node;
        }
    }
}
