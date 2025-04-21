package com.shade.decima.app.viewport;

import com.shade.decima.app.viewport.renderpass.RenderPass;
import com.shade.decima.math.Vec2;
import com.shade.decima.scene.Scene;
import com.shade.gl.awt.AWTGLCanvas;
import com.shade.gl.awt.GLData;
import com.shade.util.NotNull;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;

public final class Viewport extends JPanel {
    private static final GLDebugMessageCallback DEBUG_MESSAGE_CALLBACK = new ViewportDebugCallback();

    private final List<RenderPass> passes = new ArrayList<>();
    private final AWTGLCanvas canvas;
    private final ViewportInput input;
    private final ViewportAnimator animator;

    private float cameraSpeed = 5.f;
    private float cameraDistance = 1.f;
    private long lastUpdateTime;

    private Camera camera;
    private Scene scene;

    public Viewport() {
        super(new BorderLayout());

        GLData data = new GLData();
        data.majorVersion = 4;
        data.minorVersion = 4;
        data.swapInterval = 1;
        data.profile = GLData.Profile.CORE;

        canvas = new AWTGLCanvas(data) {
            @Override
            public void initGL() {
                // TODO: Do we need to destroy the context when the viewport is destroyed?
                GL.createCapabilities();

                // Enable depth testing
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LESS);
                glDepthMask(true);

                // Enable back face culling
                // glEnable(GL_CULL_FACE);
                // glCullFace(GL_BACK);

                // Enable debug output
                glEnable(GL_DEBUG_OUTPUT);
                glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
                glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, true);
                glDebugMessageControl(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_PUSH_GROUP, GL_DONT_CARE, 0, false);
                glDebugMessageControl(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_POP_GROUP, GL_DONT_CARE, 0, false);
                glDebugMessageCallback(DEBUG_MESSAGE_CALLBACK, 0);

                lastUpdateTime = System.currentTimeMillis();

                for (RenderPass pass : passes) {
                    pass.init();
                }
            }

            @Override
            public void paintGL() {
                var currentUpdateTime = System.currentTimeMillis();
                var currentUpdateDelta = (currentUpdateTime - lastUpdateTime) / 1000.0f;

                processInput(currentUpdateDelta);
                renderScene(currentUpdateDelta);

                lastUpdateTime = currentUpdateTime;
                swapBuffers();
            }

            @Override
            public void disposeCanvas() {
                if (initCalled) {
                    for (RenderPass pass : passes) {
                        pass.dispose();
                    }
                }
                super.disposeCanvas();
            }
        };

        input = new ViewportInput(canvas);
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.addMouseWheelListener(input);
        canvas.addKeyListener(input);
        canvas.addFocusListener(input);

        animator = new ViewportAnimator(this);

        add(canvas, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        animator.start();
    }

    @Override
    public void removeNotify() {
        animator.stop();
        super.removeNotify();
    }

    public void render() {
        canvas.render();
    }

    public boolean isKeyDown(int keyCode) {
        return input.isKeyDown(keyCode);
    }

    public boolean isMouseDown(int button) {
        return input.isMouseDown(button);
    }

    public Vec2 mousePositionDelta() {
        return input.mousePositionDelta();
    }

    public float mouseWheelDelta() {
        return input.mouseWheelDelta();
    }

    public void addRenderPass(RenderPass pass) {
        if (passes.contains(pass)) {
            throw new IllegalArgumentException("Render pass already added");
        }
        passes.add(pass);
        // TODO: Init should only be called if we have an active GL context
        // pass.init();
    }

    public void removeRenderPass(RenderPass pass) {
        if (!passes.contains(pass)) {
            throw new IllegalArgumentException("Render pass not added");
        }
        // TODO: Dispose should only be called if we have an active GL context
        // pass.dispose();
        passes.remove(pass);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public int getFramebufferWidth() {
        return canvas.getFramebufferWidth();
    }

    public int getFramebufferHeight() {
        return canvas.getFramebufferHeight();
    }

    private void renderScene(float dt) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, getFramebufferWidth(), getFramebufferHeight());

        for (RenderPass pass : passes) {
            pass.draw(this, dt);
        }
    }

    private void processInput(float dt) {
        updateCamera(dt);
        updateKeys();
        input.clear();
    }

    private void updateKeys() {
        if (input.isKeyDown(KeyEvent.VK_X)) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }

    private void updateCamera(float dt) {
        if (camera == null) {
            return;
        }

        camera.resize(getFramebufferWidth(), getFramebufferHeight());

        var sensitivity = 1.0f;
        var mouseDelta = input.mousePositionDelta().mul(sensitivity);
        var wheelDelta = input.mouseWheelDelta() * sensitivity * 0.1f;

        if (input.isMouseDown(MouseEvent.BUTTON1)) {
            cameraSpeed = Math.clamp((float) Math.exp(Math.log(cameraSpeed) + wheelDelta), 0.1f, 100.0f);
            updateFlyCamera(dt, mouseDelta);
        } else if (input.isMouseDown(MouseEvent.BUTTON2)) {
            updateCameraZoom(Math.clamp((float) Math.exp(Math.log(cameraDistance) - wheelDelta), 0.1f, 100.0f));
            // updatePan(input, dt, mouseDelta);
        } else if (input.isMouseDown(MouseEvent.BUTTON3)) {
            updateCameraZoom(Math.clamp((float) Math.exp(Math.log(cameraDistance) - wheelDelta), 0.1f, 100.0f));
            // updateOrbit(input, dt, mouseDelta);
        }
    }

    private void updateCameraZoom(float newDistance) {
        float delta = newDistance - cameraDistance;
        if (delta != 0.0f) {
            camera.position(camera.position().sub(camera.forward().mul(delta)));
            cameraDistance = newDistance;
        }
    }

    private void updateFlyCamera(float dt, @NotNull Vec2 mouse) {
        float speed = cameraSpeed * dt;
        if (input.isKeyDown(KeyEvent.VK_SHIFT)) {
            speed *= 5.0f;
        }
        if (input.isKeyDown(KeyEvent.VK_CONTROL)) {
            speed /= 5.0f;
        }

        var position = camera.position();
        var forward = camera.forward().mul(speed);
        var right = camera.right().mul(speed);

        // Horizontal movement
        if (input.isKeyDown(KeyEvent.VK_W)) {
            position = position.add(forward);
        }
        if (input.isKeyDown(KeyEvent.VK_A)) {
            position = position.sub(right);
        }
        if (input.isKeyDown(KeyEvent.VK_S)) {
            position = position.sub(forward);
        }
        if (input.isKeyDown(KeyEvent.VK_D)) {
            position = position.add(right);
        }

        // Vertical movement
        if (input.isKeyDown(KeyEvent.VK_Q)) {
            position = position.sub(0.0f, 0.0f, speed);
        }
        if (input.isKeyDown(KeyEvent.VK_E)) {
            position = position.add(0.0f, 0.0f, speed);
        }

        camera.position(position);
        camera.rotate(mouse.x(), mouse.y());
    }

}
