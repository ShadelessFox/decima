package com.shade.decima.model.viewer;

import com.formdev.flatlaf.FlatClientProperties;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.viewer.scene.Node;
import com.shade.decima.model.viewer.scene.SceneSerializer;
import com.shade.decima.ui.data.ValueController;
import com.shade.decima.ui.menu.MenuConstants;
import com.shade.gl.awt.AWTGLCanvas;
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
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
            viewport = new ModelViewport(new Camera());
            viewport.setPreferredSize(new Dimension(400, 400));
            viewport.setMinimumSize(new Dimension(100, 100));
            viewport.addPropertyChangeListener(this);
        } catch (Throwable e) {
            log.error("Can't create GL canvas", e);
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
        Disposable.dispose(loop);
        Disposable.dispose(viewport);

        loop = null;
        viewport = null;
        controller = null;
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
            viewport.setModel(new NodeModel(node, viewport));
        }
    }

    @Nullable
    public ValueController<RTTIObject> getController() {
        return controller;
    }

    private static class RenderLoop extends Thread implements Disposable {
        private final Window window;
        private final AWTGLCanvas canvas;

        private final Handler handler;

        private final AtomicBoolean isRunning = new AtomicBoolean(true);
        private final AtomicBoolean isThrottling = new AtomicBoolean(false);

        private final Lock renderLock = new ReentrantLock();
        private final Condition canRender = renderLock.newCondition();

        public RenderLoop(@NotNull Window window, @NotNull AWTGLCanvas canvas) {
            super("Render Loop");

            this.window = window;
            this.canvas = canvas;
            this.handler = new Handler();

            canvas.addHierarchyListener(handler);
            canvas.addComponentListener(handler);
            window.addWindowListener(handler);
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                try {
                    renderLock.lock();

                    while (isThrottling.get()) {
                        canRender.awaitUninterruptibly();
                    }
                } finally {
                    renderLock.unlock();
                }

                try {
                    SwingUtilities.invokeAndWait(() -> {
                        beforeRender();

                        if (canvas.isValid()) {
                            canvas.render();
                        }

                        afterRender();
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }

            canvas.removeHierarchyListener(handler);
            window.removeWindowListener(handler);
        }

        @Override
        public void dispose() {
            isRunning.set(false);
        }

        protected void beforeRender() {
            // do nothing by default
        }

        protected void afterRender() {
            // do nothing by default
        }

        private class Handler extends WindowAdapter implements HierarchyListener, ComponentListener {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    handle();
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                handle();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                handle();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                handle();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                handle();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                handleAsync();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                handleAsync();
            }

            private void handleAsync() {
                SwingUtilities.invokeLater(this::handle);
            }

            private void handle() {
                renderLock.lock();

                try {
                    isThrottling.set(isThrottling());
                    canRender.signal();
                } finally {
                    renderLock.unlock();
                }
            }

            private boolean isThrottling() {
                return !canvas.isShowing() || canvas.getWidth() <= 0 || canvas.getHeight() <= 0 || !isActive(window);
            }

            private static boolean isActive(@NotNull Window window) {
                if (window instanceof Dialog dialog && dialog.getModalityType() != Dialog.ModalityType.MODELESS) {
                    return false;
                }

                if (window.isActive()) {
                    return true;
                }

                for (Window ownedWindow : window.getOwnedWindows()) {
                    if (isActive(ownedWindow)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }
}
