package com.shade.decima.model.viewer;

import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.viewer.outline.OutlineDialog;
import com.shade.decima.model.viewer.renderer.DebugRenderer;
import com.shade.decima.model.viewer.renderer.GridRenderer;
import com.shade.decima.model.viewer.renderer.ModelRenderer;
import com.shade.decima.model.viewer.renderer.OutlineRenderer;
import com.shade.decima.model.viewer.scene.Node;
import com.shade.gl.DebugGroup;
import com.shade.gl.awt.AWTGLCanvas;
import com.shade.gl.awt.GLData;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

public class ModelViewport extends AWTGLCanvas implements Disposable {
    public static final DataKey<ModelViewport> VIEWPORT_KEY = new DataKey<>("viewport", ModelViewport.class);
    private static final Logger log = LoggerFactory.getLogger(ModelViewport.class);

    private final Handler handler;
    private final OutlineRenderer outlineRenderer;
    private final GridRenderer gridRenderer;
    private final ModelRenderer modelRenderer;
    private final DebugRenderer debugRenderer;
    private final Camera camera;

    private long lastFrameTime;
    private boolean showWireframe;
    private boolean showNormals;
    private boolean softShading = true;

    private OutlineDialog outlineDialog;
    private Model model;

    public ModelViewport(@NotNull Camera camera) {
        super(createData());

        Robot robot = null;

        try {
            robot = new Robot();
        } catch (AWTException e) {
            log.warn("Can't create robot", e);
        }

        this.handler = new Handler(robot);
        this.outlineRenderer = new OutlineRenderer();
        this.gridRenderer = new GridRenderer();
        this.modelRenderer = new ModelRenderer();
        this.debugRenderer = new DebugRenderer();
        this.camera = camera;

        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
        addKeyListener(handler);
        addFocusListener(handler);

        setBackground(Color.BLACK);
    }

    @NotNull
    private static GLData createData() {
        final GLData data = new GLData();
        data.majorVersion = 4;
        data.minorVersion = 3;
        data.profile = GLData.Profile.CORE;
        return data;
    }

    @Override
    public void initGL() {
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);

        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, true);
        glDebugMessageControl(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_PUSH_GROUP, GL_DONT_CARE, 0, false);
        glDebugMessageControl(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_POP_GROUP, GL_DONT_CARE, 0, false);
        glDebugMessageCallback(new DebugCallback(), 0);

        try {
            outlineRenderer.setup();
            gridRenderer.setup();
            modelRenderer.setup();
            debugRenderer.setup();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        modelRenderer.setModel(model);
    }

    @Override
    public void paintGL() {
        final double scaleFactor = UIScale.getSystemScaleFactor(getGraphicsConfiguration());
        final int width = (int) (getWidth() * scaleFactor);
        final int height = (int) (getHeight() * scaleFactor);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);

        final long currentFrameTime = System.currentTimeMillis();
        final float delta = (currentFrameTime - lastFrameTime) / 1000.0f;

        camera.resize(width, height);
        camera.update(handler, delta);
        handler.clear();

        outlineRenderer.bind(width, height);

        try (var ignored = new DebugGroup("Render Model")) {
            modelRenderer.setSelectionOnly(true);
            modelRenderer.render(delta, this);

            modelRenderer.setSelectionOnly(false);
            modelRenderer.render(delta, this);
        }

        try (var ignored = new DebugGroup("Render Grid")) {
            gridRenderer.render(delta, this);
        }

        try (var ignored = new DebugGroup("Render Lines")) {
            if (handler.isMouseDown(MouseEvent.BUTTON2) || handler.isMouseDown(MouseEvent.BUTTON3)) {
                final Vector3f target = camera.getTarget();
                debugRenderer.cross(target, 0.1f, false);
                debugRenderer.circle(target, camera.getForwardVector(), new Vector3f(1.0f, 1.0f, 0.0f), 0.05f, 8, false);
            }

            debugRenderer.render(delta, this);
        }

        outlineRenderer.unbind();
        outlineRenderer.render(delta, this);

        lastFrameTime = currentFrameTime;
        swapBuffers();
    }

    @Override
    public void removeNotify() {
        if (initCalled) {
            outlineRenderer.dispose();
            gridRenderer.dispose();
            modelRenderer.dispose();
            debugRenderer.dispose();

            glDisable(GL_DEBUG_OUTPUT);
            glDisable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
            glDebugMessageCallback(null, 0);
            glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, false);
        }

        super.removeNotify();
    }

    @Override
    public void dispose() {
        if (outlineDialog != null) {
            outlineDialog.dispose();
            outlineDialog = null;
        }

        // Will be disposed in ModelRenderer#dispose()
        model = null;
    }

    public boolean isShowOutline() {
        return outlineDialog != null && outlineDialog.isVisible();
    }

    public boolean isShowOutlineFor(@NotNull Node node) {
        return isShowOutline() && outlineDialog.getSelection().contains(node);
    }

    public void setShowOutline(boolean visible) {
        if (outlineDialog == null) {
            outlineDialog = new OutlineDialog(
                JOptionPane.getRootFrame(),
                ((NodeModel) Objects.requireNonNull(getModel())).getRoot()
            );
            outlineDialog.addWindowListener(new WindowAdapter() {
                private boolean activated;

                @Override
                public void windowActivated(WindowEvent e) {
                    firePropertyChange("showOutline", activated, true);
                    activated = true;
                }

                @Override
                public void windowDeactivated(WindowEvent e) {
                    firePropertyChange("showOutline", activated, outlineDialog.isVisible());
                    activated = outlineDialog.isVisible();
                }
            });
        }

        outlineDialog.setVisible(visible);
    }

    @NotNull
    public Camera getCamera() {
        return camera;
    }

    @Nullable
    public Model getModel() {
        return model;
    }

    public void setModel(@Nullable Model model) {
        final Model oldModel = this.model;

        if (oldModel != model) {
            modelRenderer.setModel(model);
            this.model = model;
            firePropertyChange("model", oldModel, model);
        }
    }

    public boolean isShowWireframe() {
        return showWireframe;
    }

    public void setShowWireframe(boolean showWireframe) {
        this.showWireframe = showWireframe;
    }

    public boolean isShowNormals() {
        return showNormals;
    }

    public void setShowNormals(boolean showNormals) {
        this.showNormals = showNormals;
    }

    public boolean isSoftShading() {
        return softShading;
    }

    public void setSoftShading(boolean softShading) {
        this.softShading = softShading;
    }

    private class Handler extends MouseAdapter implements KeyListener, FocusListener, InputState {
        private final Robot robot;
        private final Set<Integer> mouseState = new HashSet<>();
        private final Set<Integer> keyState = new HashSet<>(3);

        private final Point mouseStart = new Point();
        private final Point mouseRecent = new Point();
        private final Point mouseDelta = new Point();
        private float mouseWheelDelta;

        public Handler(@Nullable Robot robot) {
            this.robot = robot;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            mouseState.add(e.getButton());
            mouseStart.setLocation(e.getPoint());
            mouseRecent.setLocation(mouseStart);
            mouseDelta.setLocation(0, 0);
            SwingUtilities.convertPointToScreen(mouseRecent, ModelViewport.this);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseState.remove(e.getButton());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final Point mouse = e.getLocationOnScreen();
            final Rectangle bounds = new Rectangle(getLocationOnScreen(), getSize());

            // Shrink the bounds in case the window is maximized so the mouse can move out of bounds there
            bounds.width -= 1;
            bounds.height -= 1;

            if (robot != null && !bounds.contains(mouse)) {
                mouse.x = MathUtils.wrapAround(mouse.x, bounds.x, bounds.x + bounds.width);
                mouse.y = MathUtils.wrapAround(mouse.y, bounds.y, bounds.y + bounds.height);

                robot.mouseMove(mouse.x, mouse.y);
                mouseRecent.setLocation(mouse.x, mouse.y);
            } else {
                mouseDelta.x += mouse.x - mouseRecent.x;
                mouseDelta.y += mouse.y - mouseRecent.y;
                mouseRecent.setLocation(mouse);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            mouseWheelDelta -= (float) e.getPreciseWheelRotation();
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // do nothing
        }

        @Override
        public void keyPressed(KeyEvent e) {
            keyState.add(e.getKeyCode());
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyState.remove(e.getKeyCode());
        }

        @Override
        public void focusGained(FocusEvent e) {
            // do nothing
        }

        @Override
        public void focusLost(FocusEvent e) {
            keyState.clear();
            mouseState.clear();
            setCursor(null);
        }

        @Override
        public boolean isKeyDown(int keyCode) {
            return keyState.contains(keyCode);
        }

        @Override
        public boolean isMouseDown(int mouseButton) {
            return mouseState.contains(mouseButton);
        }

        @NotNull
        @Override
        public Vector2f getMousePositionDelta() {
            return new Vector2f(mouseDelta.x, mouseDelta.y);
        }

        @Override
        public float getMouseWheelRotationDelta() {
            return mouseWheelDelta;
        }

        private void clear() {
            mouseDelta.setLocation(0, 0);
            mouseWheelDelta = 0.0f;
        }
    }

    private static class DebugCallback extends GLDebugMessageCallback {
        @Override
        public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
            final String sourceString = switch (source) {
                case GL_DEBUG_SOURCE_API -> "API";
                case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW_SYSTEM";
                case GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER_COMPILER";
                case GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY";
                case GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION";
                case GL_DEBUG_SOURCE_OTHER -> "OTHER";
                default -> "unknown";
            };

            final String typeString = switch (type) {
                case GL_DEBUG_TYPE_ERROR -> "ERROR";
                case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED_BEHAVIOR";
                case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED_BEHAVIOR";
                case GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY";
                case GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE";
                case GL_DEBUG_TYPE_MARKER -> "MARKER";
                case GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP";
                case GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP";
                case GL_DEBUG_TYPE_OTHER -> "OTHER";
                default -> "unknown";
            };

            final String severityString = switch (severity) {
                case GL_DEBUG_SEVERITY_LOW -> "LOW";
                case GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM";
                case GL_DEBUG_SEVERITY_HIGH -> "HIGH";
                case GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION";
                default -> "unknown";
            };

            log.debug("[source: {}, type: {}, severity: {}]: {}", sourceString, typeString, severityString, getMessage(length, message));
        }
    }
}
