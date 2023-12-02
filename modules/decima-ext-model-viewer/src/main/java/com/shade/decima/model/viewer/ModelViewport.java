package com.shade.decima.model.viewer;

import com.formdev.flatlaf.util.UIScale;
import com.shade.decima.model.viewer.isr.Node;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
import com.shade.decima.model.viewer.outline.OutlineDialog;
import com.shade.decima.model.viewer.renderer.ModelRenderer;
import com.shade.decima.model.viewer.renderer.OutlineRenderer;
import com.shade.decima.model.viewer.renderer.ViewportRenderer;
import com.shade.platform.model.Disposable;
import com.shade.platform.model.data.DataKey;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
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
import java.util.Objects;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.*;

public class ModelViewport extends AWTGLCanvas implements Disposable {
    public static final DataKey<ModelViewport> VIEWPORT_KEY = new DataKey<>("viewport", ModelViewport.class);
    private static final Logger log = LoggerFactory.getLogger(ModelViewport.class);

    private final Handler handler;
    private final OutlineRenderer outlineRenderer;
    private final ViewportRenderer viewportRenderer;
    private final ModelRenderer modelRenderer;
    private final Camera camera;

    private long lastFrameTime;
    private boolean showWireframe;
    private boolean showNormals;
    private boolean softShading = true;

    private OutlineDialog outlineDialog;

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
        this.viewportRenderer = new ViewportRenderer();
        this.modelRenderer = new ModelRenderer();
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

        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, true);
        glDebugMessageCallback(new DebugCallback(), 0);

        try {
            outlineRenderer.setup();
            viewportRenderer.setup();
            modelRenderer.setup();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        camera.update(delta, handler);

        viewportRenderer.update(delta, handler, this);

        outlineRenderer.bind(width, height);

        {
            modelRenderer.setSelectionOnly(true);
            modelRenderer.update(delta, handler, this);

            modelRenderer.setSelectionOnly(false);
            modelRenderer.update(delta, handler, this);
        }

        outlineRenderer.unbind();
        outlineRenderer.update(delta, handler, this);

        lastFrameTime = currentFrameTime;
        swapBuffers();
    }

    public boolean isSelected(@NotNull Node node) {
        return outlineDialog != null && outlineDialog.getSelection().contains(node);
    }

    @Override
    public void dispose() {
        outlineRenderer.dispose();
        viewportRenderer.dispose();
        modelRenderer.dispose();

        if (outlineDialog != null) {
            outlineDialog.dispose();
            outlineDialog = null;
        }

        if (!initCalled) {
            return;
        }

        glDisable(GL_DEBUG_OUTPUT);
        glDisable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback(null, 0);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, false);
    }

    public boolean isShowOutline() {
        return outlineDialog != null && outlineDialog.isVisible();
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
        return modelRenderer.getModel();
    }

    public void setModel(@Nullable Model model) {
        final Model oldModel = modelRenderer.getModel();

        if (oldModel != model) {
            modelRenderer.setModel(model);
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

    private class Handler extends MouseAdapter implements KeyListener, InputHandler, FocusListener {
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
                SwingUtilities.convertPointToScreen(point, ModelViewport.this);

                robot.mouseMove(point.x, point.y);
                position.add(e.getX(), e.getY()).sub(origin);
            } else {
                position.set(e.getX(), e.getY());
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            wheelRotation -= (float) e.getPreciseWheelRotation();
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
