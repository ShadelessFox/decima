package com.shade.decima.model.viewer;

import com.shade.decima.model.viewer.mesh.Mesh;
import com.shade.decima.model.viewer.renderer.MeshRenderer;
import com.shade.decima.model.viewer.renderer.ViewportRenderer;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL15.*;

public class MeshViewerCanvas extends AWTGLCanvas implements Disposable {
    public static final DataKey<MeshViewerCanvas> CANVAS_KEY = new DataKey<>("canvas", MeshViewerCanvas.class);
    private static final Logger log = LoggerFactory.getLogger(MeshViewerCanvas.class);

    private final Handler handler;
    private final ViewportRenderer viewportRenderer;
    private final MeshRenderer meshRenderer;

    private long lastFrameTime;
    private boolean showWireframe;

    public MeshViewerCanvas(@NotNull Camera camera) {
        Robot robot = null;

        try {
            robot = new Robot();
        } catch (AWTException e) {
            log.warn("Can't create robot", e);
        }

        this.handler = new Handler(robot);
        this.viewportRenderer = new ViewportRenderer();
        this.meshRenderer = new MeshRenderer(camera);

        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
        addKeyListener(handler);

        setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    public void initGL() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        try {
            viewportRenderer.setup();
            meshRenderer.setup();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void paintGL() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, getWidth(), getHeight());

        final long currentFrameTime = System.currentTimeMillis();
        final float delta = (currentFrameTime - lastFrameTime) / 1000.0f;

        viewportRenderer.update(delta, handler, this);
        meshRenderer.update(delta, handler, this);
        lastFrameTime = currentFrameTime;

        swapBuffers();
    }

    @Override
    public void dispose() {
        viewportRenderer.dispose();
        meshRenderer.dispose();
    }

    public void setMesh(@Nullable Mesh mesh) {
        meshRenderer.setMesh(mesh);
    }

    public boolean isShowWireframe() {
        return showWireframe;
    }

    public void setShowWireframe(boolean showWireframe) {
        if (this.showWireframe != showWireframe) {
            this.showWireframe = showWireframe;
            firePropertyChange("showWireframe", !showWireframe, showWireframe);
        }
    }

    private class Handler extends MouseAdapter implements KeyListener, InputHandler {
        private static final Cursor EMPTY_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
            new Point(0, 0),
            "empty cursor"
        );

        private final Robot robot;
        private final Map<Integer, Boolean> mouseState = new HashMap<>();
        private final Map<Integer, Boolean> keyState = new HashMap<>();
        private final Vector2f origin = new Vector2f();
        private final Vector2f position = new Vector2f();
        private float wheelRotation = 0.0f;

        public Handler(@Nullable Robot robot) {
            this.robot = robot;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mouseState.put(e.getButton(), true);
            origin.set(e.getX(), e.getY());
            position.set(e.getX(), e.getY());

            if (robot != null) {
                setCursor(EMPTY_CURSOR);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseState.put(e.getButton(), false);

            if (robot != null) {
                setCursor(null);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (robot != null) {
                final Point point = new Point((int) origin.x, (int) origin.y);
                SwingUtilities.convertPointToScreen(point, MeshViewerCanvas.this);

                robot.mouseMove(point.x, point.y);
                position.add(e.getX(), e.getY()).sub(origin);
            } else {
                position.set(e.getX(), e.getY());
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            wheelRotation -= e.getPreciseWheelRotation();
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // do nothing
        }

        @Override
        public void keyPressed(KeyEvent e) {
            keyState.put(e.getKeyCode(), true);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyState.put(e.getKeyCode(), false);
        }

        @Override
        public boolean isKeyDown(int keyCode) {
            return keyState.getOrDefault(keyCode, false);
        }

        @Override
        public boolean isMouseDown(int mouseButton) {
            return mouseState.getOrDefault(mouseButton, false);
        }

        @NotNull
        @Override
        public Vector2f getMouseOrigin() {
            return new Vector2f(origin);
        }

        @NotNull
        @Override
        public Vector2f getMousePosition() {
            return new Vector2f(position);
        }

        @Override
        public float getMouseWheelRotation() {
            return wheelRotation;
        }
    }
}
