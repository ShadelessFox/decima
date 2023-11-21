package com.shade.decima.ui.data.viewer.model;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatHelpButtonIcon;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.decima.model.viewer.RenderLoop;
import com.shade.decima.model.viewer.camera.FirstPersonCamera;
import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.data.viewer.model.isr.SceneSerializer;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.ui.dialogs.ProgressDialog;
import com.shade.platform.ui.menus.MenuManager;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;

public class ModelViewerPanel extends JComponent implements Disposable, PropertyChangeListener {
    public static final DataKey<ModelViewerPanel> PANEL_KEY = new DataKey<>("panel", ModelViewerPanel.class);

    private static final Logger log = LoggerFactory.getLogger(ModelViewerPanel.class);

    private JToolBar topToolbar;
    private JToolBar bottomToolbar;
    private MeshViewerCanvas canvas;
    private RenderLoop loop;

    private ValueController<RTTIObject> controller;

    public ModelViewerPanel() {
        bottomToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_MODEL_VIEWER_BOTTOM_ID, key -> switch (key) {
            case "panel" -> this;
            default -> null;
        });

        bottomToolbar.add(Box.createHorizontalGlue());
        bottomToolbar.add(new AbstractAction(null, new FlatHelpButtonIcon()) {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    Desktop.getDesktop().browse(URI.create("https://github.com/ShadelessFox/decima/wiki/Model-export"));
                } catch (IOException e) {
                    UIUtils.showErrorDialog(e, "Unable to open wiki page");
                }
            }
        });

        setLayout(new BorderLayout());
        add(bottomToolbar, BorderLayout.SOUTH);

        try {
            canvas = new MeshViewerCanvas(new FirstPersonCamera());
            canvas.setPreferredSize(new Dimension(400, 400));
            canvas.setMinimumSize(new Dimension(100, 100));
            canvas.addPropertyChangeListener(this);
        } catch (Throwable e) {
            log.error("Can't create GL canvas: " + e.getMessage());
        }

        if (canvas != null) {
            final JLabel statusLabel = new JLabel();
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

            topToolbar = MenuManager.getInstance().createToolBar(this, MenuConstants.BAR_MODEL_VIEWER_ID, key -> switch (key) {
                case "canvas" -> canvas;
                default -> null;
            });
            topToolbar.add(Box.createHorizontalGlue());
            topToolbar.add(statusLabel);

            final JPanel canvasHolder = new JPanel();
            canvasHolder.setLayout(new BorderLayout());
            canvasHolder.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.shadow")));
            canvasHolder.add(canvas, BorderLayout.CENTER);

            add(topToolbar, BorderLayout.NORTH);
            add(canvasHolder, BorderLayout.CENTER);

            loop = new RenderLoop(JOptionPane.getRootFrame(), canvas) {
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
        final String name = event.getPropertyName();

        if (name.equals("background") || name.equals("controller")) {
            MenuManager.getInstance().update(topToolbar);
            MenuManager.getInstance().update(bottomToolbar);
        }
    }

    @Override
    public void dispose() {
        if (canvas != null) {
            loop.dispose();
            canvas.dispose();
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
        if (canvas == null) {
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
            canvas.setModel(new NodeModel(node));
        }
    }

    @Nullable
    public ValueController<RTTIObject> getController() {
        return controller;
    }
}
