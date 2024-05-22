package com.shade.decima.model.viewer;

import com.shade.decima.model.viewer.settings.ModelViewerSettings;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import org.joml.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.Math;

public class Camera {
    private final Vector3f location = new Vector3f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Vector2f windowSize = new Vector2f();

    private float pitch;
    private float yaw;
    private float aspectRatio;

    public Camera() {
        location.set(1.0f);
        lookAt(new Vector3f());
    }

    public void update(float dt, @NotNull InputState input) {
        final float speed = 5.0f * dt;
        final float sensitivity = ModelViewerSettings.getInstance().sensitivity;
        final Vector2f mouseDelta = input.getMousePositionDelta().mul(sensitivity);

        if (input.isMouseDown(MouseEvent.BUTTON1)) {
            // Directional movement
            if (input.isKeyDown(KeyEvent.VK_W)) location.add(getForwardVector().mul(speed));
            if (input.isKeyDown(KeyEvent.VK_A)) location.sub(getRightVector().mul(speed));
            if (input.isKeyDown(KeyEvent.VK_S)) location.sub(getForwardVector().mul(speed));
            if (input.isKeyDown(KeyEvent.VK_D)) location.add(getRightVector().mul(speed));

            // Vertical movement
            if (input.isKeyDown(KeyEvent.VK_Q)) location.sub(new Vector3f(0.0f, 0.0f, speed));
            if (input.isKeyDown(KeyEvent.VK_E)) location.add(new Vector3f(0.0f, 0.0f, speed));

            yaw -= (float) (Math.PI * mouseDelta.x / windowSize.x);
            pitch -= (float) (Math.PI / aspectRatio * mouseDelta.y / windowSize.y);

            clampRotation();
            recalculateMatrices();
        } else if (input.isMouseDown(MouseEvent.BUTTON2)) {
            location.sub(getRightVector().mul(mouseDelta.x * speed));
            location.add(getUpVector().mul(mouseDelta.y * speed));
        }
    }

    public void resize(int width, int height) {
        final ModelViewerSettings settings = ModelViewerSettings.getInstance();

        windowSize.set(width, height);
        aspectRatio = (float) width / height;
        projectionMatrix.setPerspective(getFieldOfView(), aspectRatio, settings.nearClip, settings.farClip);
        recalculateMatrices();
    }

    @NotNull
    public Vector3fc getPosition() {
        return location;
    }

    public void setPosition(@NotNull Vector3fc position) {
        this.location.set(position);
        recalculateMatrices();
    }

    public void lookAt(@NotNull Vector3fc target) {
        final Vector3f dir = target.sub(location, new Vector3f()).normalize();
        yaw = (float) Math.atan2(dir.y, dir.x);
        pitch = (float) Math.asin(dir.z);

        clampRotation();
        recalculateMatrices();
    }

    @NotNull
    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

    @NotNull
    public Matrix4fc getProjectionMatrix() {
        return projectionMatrix;
    }

    @NotNull
    private Vector3f getForwardVector() {
        final float yawSin = (float) Math.sin(yaw);
        final float yawCos = (float) Math.cos(yaw);
        final float pitchSin = (float) Math.sin(pitch);
        final float pitchCos = (float) Math.cos(pitch);
        return new Vector3f(yawCos * pitchCos, yawSin * pitchCos, pitchSin);
    }

    @NotNull
    private Vector3f getRightVector() {
        final float x = (float) Math.cos(yaw - MathUtils.HALF_PI);
        final float y = (float) Math.sin(yaw - MathUtils.HALF_PI);
        return new Vector3f(x, y, 0);
    }

    @NotNull
    private Vector3f getUpVector() {
        return getRightVector().cross(getForwardVector());
    }

    private static float getFieldOfView() {
        return (float) (ModelViewerSettings.getInstance().fieldOfView * Math.PI / 180.0f);
    }

    private void recalculateMatrices() {
        viewMatrix.setLookAt(location, getForwardVector().add(location), new Vector3f(0.0f, 0.0f, 1.0f));
    }

    private void clampRotation() {
        float limit = (float) (89.5f * Math.PI / 180.0f);
        pitch = MathUtils.clamp(pitch, -limit, limit);
    }
}
