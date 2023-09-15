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
    private static final float FOV = 45.0f;
    private static final float SENSITIVITY = 0.2f;
    private static final float CLIP_NEAR = 0.01f;
    private static final float CLIP_FAR = 1000.0f;
    private static final float SPEED_FACTOR = 5.0f;

    private final Vector3f position = new Vector3f(-0.5f, 0.5f, 0.0f);
    private final Vector3f direction = new Vector3f(1.0f, 0.0f, 0.0f);

    private final Vector2f lastMouseOrigin = new Vector2f();
    private final Vector2f lastMousePosition = new Vector2f();
    private float lastWheelRotation = 0.0f;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private float speed = 1.0f;

    @Override
    public void update(float dt, @NotNull InputHandler input) {
        if (input.isMouseDown(MouseEvent.BUTTON1)) {
            final Vector2f mouse = input.getMousePosition();
            final Vector2f origin = input.getMouseOrigin();
            final Vector2f delta = new Vector2f();

            if (lastMouseOrigin.equals(origin)) {
                delta.x = mouse.x - lastMousePosition.x;
                delta.y = lastMousePosition.y - mouse.y;
                delta.mul(SENSITIVITY);
            }

            lastMousePosition.set(mouse);
            lastMouseOrigin.set(origin);

            if (delta.x != 0.0f || delta.y != 0.0f) {
                yaw = (yaw + delta.x) % 360;
                pitch = IOUtils.clamp(pitch + delta.y, -89.0f, 89.0f);

                direction.x = (float) (Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw)));
                direction.y = (float) (Math.sin(Math.toRadians(pitch)));
                direction.z = (float) (Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)));
                direction.normalize();
            }
        } else {
            lastMouseOrigin.set(input.getMousePosition());
        }

        final float oldWheelRotation = lastWheelRotation;
        final float newWheelRotation = input.getMouseWheelRotation() * 0.2f;

        if (newWheelRotation != oldWheelRotation) {
            lastWheelRotation = newWheelRotation;
            speed = IOUtils.clamp((float) Math.exp(Math.log(speed) + newWheelRotation - oldWheelRotation), 0.01f, 10.0f);
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
        }

        if (input.isKeyDown(KeyEvent.VK_W) || input.isKeyDown(KeyEvent.VK_S)) {
            final Vector3f forward = new Vector3f(direction).mul(speed);

            if (input.isKeyDown(KeyEvent.VK_W)) {
                position.add(forward);
            } else {
                position.sub(forward);
            }
        }

        if (input.isKeyDown(KeyEvent.VK_A) || input.isKeyDown(KeyEvent.VK_D)) {
            final Vector3f right = new Vector3f(direction).cross(UP).normalize().mul(speed);

            if (input.isKeyDown(KeyEvent.VK_D)) {
                position.add(right);
            } else {
                position.sub(right);
            }
        }

        viewMatrix.setLookAt(position, new Vector3f(position).add(direction), UP);
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
}
