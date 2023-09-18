package com.shade.decima.model.viewer.camera;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.InputHandler;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.joml.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.Math;

public class FirstPersonCamera implements Camera {
    private static final Vector3fc UP = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3fc FORWARD = new Vector3f(0.0f, 0.0f, 1.0f);

    private static final float FOV = 45.0f;
    private static final float SENSITIVITY = 0.2f;
    private static final float CLIP_NEAR = 0.01f;
    private static final float CLIP_FAR = 1000.0f;
    private static final float SPEED_FACTOR = 5.0f;

    private final Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
    private final Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);
    private final Quaternionf rotation = new Quaternionf();

    private final Vector2f lastMouseOrigin = new Vector2f();
    private final Vector2f lastMousePosition = new Vector2f();
    private float lastWheelRotation = 0.0f;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private float speed = 1.0f;
    private float distance = 1.0f;

    public FirstPersonCamera() {
        // Tilts camera a bit
        pitch = -15.0f;
        distance = 2.0f;

        updateRotation();
        updatePositionFromTarget();
    }

    @Override
    public void update(float dt, @NotNull InputHandler input) {
        final CameraMode mode;

        if (input.isMouseDown(MouseEvent.BUTTON1)) {
            mode = CameraMode.FPS;
        } else if (input.isMouseDown(MouseEvent.BUTTON2)) {
            mode = CameraMode.PAN;
        } else if (input.isMouseDown(MouseEvent.BUTTON3)) {
            mode = CameraMode.ARCBALL;
        } else {
            mode = null;
        }

        final float oldWheelRotation = lastWheelRotation;
        final float newWheelRotation = input.getMouseWheelRotation() * 0.2f;
        final float wheelDelta = newWheelRotation - oldWheelRotation;
        lastWheelRotation = newWheelRotation;

        if (mode != null) {
            final Vector2f mouse = input.getMousePosition();
            final Vector2f origin = input.getMouseOrigin();
            final Vector2f mouseDelta = new Vector2f();

            if (lastMouseOrigin.equals(origin)) {
                mouseDelta.x = mouse.x - lastMousePosition.x;
                mouseDelta.y = lastMousePosition.y - mouse.y;
                mouseDelta.mul(SENSITIVITY);
            }

            switch (mode) {
                case FPS -> updateFps(input, mouseDelta, wheelDelta, dt);
                case PAN -> updatePan(input, mouseDelta, wheelDelta, dt);
                case ARCBALL -> updateArcball(input, mouseDelta, wheelDelta, dt);
            }

            lastMousePosition.set(mouse);
            lastMouseOrigin.set(origin);
        } else {
            lastMouseOrigin.set(input.getMousePosition());
        }

        viewMatrix.setLookAt(position, target, UP);
    }

    @Override
    public void resize(int width, int height) {
        projectionMatrix.setPerspective((float) Math.toRadians(FOV), (float) width / height, CLIP_NEAR, CLIP_FAR);
    }

    @NotNull
    @Override
    public Vector3fc getPosition() {
        return position;
    }

    @NotNull
    @Override
    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

    @NotNull
    @Override
    public Matrix4fc getProjectionMatrix() {
        return projectionMatrix;
    }

    private void updateFps(@NotNull InputHandler input, @NotNull Vector2fc mouseDelta, float wheelDelta, float dt) {
        boolean updateTarget = false;

        if (mouseDelta.x() != 0.0f || mouseDelta.y() != 0.0f) {
            yaw = (yaw + mouseDelta.x()) % 360;
            pitch = IOUtils.clamp(pitch + mouseDelta.y(), -89.0f, 89.0f);

            updateRotation();
            updateTarget = true;
        }

        if (wheelDelta != 0.0f) {
            speed = IOUtils.clamp((float) Math.exp(Math.log(speed) + wheelDelta), 0.01f, 10.0f);
        }

        float speed = this.speed * dt;

        if (input.isKeyDown(KeyEvent.VK_SHIFT)) {
            speed *= SPEED_FACTOR;
        }

        if (input.isKeyDown(KeyEvent.VK_CONTROL)) {
            speed /= SPEED_FACTOR;
        }

        if (input.isKeyDown(KeyEvent.VK_Q) || input.isKeyDown(KeyEvent.VK_E)) {
            final Vector3f up = new Vector3f(UP).mul(speed);

            if (input.isKeyDown(KeyEvent.VK_E)) {
                position.add(up);
            } else {
                position.sub(up);
            }

            updateTarget = true;
        }

        if (input.isKeyDown(KeyEvent.VK_W) || input.isKeyDown(KeyEvent.VK_S)) {
            final Vector3f forward = getForwardVector().mul(speed);

            if (input.isKeyDown(KeyEvent.VK_W)) {
                position.add(forward);
            } else {
                position.sub(forward);
            }

            updateTarget = true;
        }

        if (input.isKeyDown(KeyEvent.VK_A) || input.isKeyDown(KeyEvent.VK_D)) {
            final Vector3f right = getForwardVector().cross(UP).normalize().mul(speed);

            if (input.isKeyDown(KeyEvent.VK_D)) {
                position.add(right);
            } else {
                position.sub(right);
            }

            updateTarget = true;
        }

        if (updateTarget) {
            distance = target.distance(position);
            target.set(position).add(getForwardVector().mul(distance));
        }
    }

    private void updatePan(@NotNull InputHandler input, @NotNull Vector2fc mouseDelta, float wheelDelta, float dt) {
        if (mouseDelta.x() != 0.0f || mouseDelta.y() != 0.0f) {
            final Vector3f forward = getForwardVector();

            if (input.isKeyDown(KeyEvent.VK_SHIFT)) {
                position.add(forward.mul(mouseDelta.y(), new Vector3f()));
            } else {
                final Vector3f right = forward.cross(UP, new Vector3f()).normalize();
                final Vector3f up = forward.cross(right, new Vector3f()).normalize();

                position.sub(right.mul(mouseDelta.x() / SPEED_FACTOR));
                position.add(up.mul(mouseDelta.y() / SPEED_FACTOR));
            }

            updateTargetFromPosition();
        }
    }

    private void updateArcball(@NotNull InputHandler input, @NotNull Vector2fc mouseDelta, float wheelDelta, float dt) {
        boolean updatePosition = false;

        if (mouseDelta.x() != 0.0f || mouseDelta.y() != 0.0f) {
            yaw = (yaw + mouseDelta.x()) % 360;
            pitch = IOUtils.clamp(pitch + mouseDelta.y(), -89.0f, 89.0f);

            updateRotation();
            updatePosition = true;
        }

        if (wheelDelta != 0.0f) {
            distance = Math.max(0.0f, (float) Math.exp(Math.log(distance) - wheelDelta));
            updatePosition = true;
        }

        if (updatePosition) {
            updatePositionFromTarget();
        }
    }

    private void updateTargetFromPosition() {
        target.set(position).add(getForwardVector().mul(distance));
    }

    private void updatePositionFromTarget() {
        position.set(target).sub(getForwardVector().mul(distance));
    }

    private void updateRotation() {
        rotation.identity();
        rotation.rotateY((float) -Math.toRadians(yaw));
        rotation.rotateX((float) -Math.toRadians(pitch));
    }

    @NotNull
    private Vector3f getForwardVector() {
        return FORWARD.rotate(rotation, new Vector3f());
    }

    private enum CameraMode {
        FPS,
        PAN,
        ARCBALL
    }
}
