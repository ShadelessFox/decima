package com.shade.decima.model.viewer;

import com.shade.decima.model.viewer.settings.ModelViewerSettings;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import org.joml.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.Math;

public class Camera {
    private static final Vector3f UP = new Vector3f(0.0f, 0.0f, 1.0f);

    private final Vector3f position = new Vector3f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Vector2f windowSize = new Vector2f();

    private float pitch;
    private float yaw;
    private float distance;
    private float speed;
    private float aspectRatio;

    public Camera() {
        position.set(5.0f);
        speed = 5.0f;
        lookAt(new Vector3f());
    }

    public void update(@NotNull InputState input, float dt) {
        final var sensitivity = ModelViewerSettings.getInstance().sensitivity;
        final var mouseDelta = input.getMousePositionDelta().mul(sensitivity);
        final var wheelDelta = input.getMouseWheelRotationDelta() * sensitivity * 0.1f;

        if (input.isMouseDown(MouseEvent.BUTTON1)) {
            speed = MathUtils.clamp((float) Math.exp(Math.log(speed) + wheelDelta), 0.1f, 100.0f);
            updateFly(input, dt, mouseDelta);
        } else if (input.isMouseDown(MouseEvent.BUTTON2)) {
            zoom(MathUtils.clamp((float) Math.exp(Math.log(distance) - wheelDelta), 0.1f, 100.0f));
            updatePan(input, dt, mouseDelta);
        } else if (input.isMouseDown(MouseEvent.BUTTON3)) {
            zoom(MathUtils.clamp((float) Math.exp(Math.log(distance) - wheelDelta), 0.1f, 100.0f));
            updateOrbit(input, dt, mouseDelta);
        }
    }

    private void updateFly(@NotNull InputState input, float dt, @NotNull Vector2f mouse) {
        float speed = this.speed * dt;
        if (input.isKeyDown(KeyEvent.VK_SHIFT)) speed *= 5.0f;
        if (input.isKeyDown(KeyEvent.VK_CONTROL)) speed /= 5.0f;

        // Directional movement
        if (input.isKeyDown(KeyEvent.VK_W)) position.add(getForwardVector().mul(speed));
        if (input.isKeyDown(KeyEvent.VK_A)) position.sub(getRightVector().mul(speed));
        if (input.isKeyDown(KeyEvent.VK_S)) position.sub(getForwardVector().mul(speed));
        if (input.isKeyDown(KeyEvent.VK_D)) position.add(getRightVector().mul(speed));

        // Vertical movement
        if (input.isKeyDown(KeyEvent.VK_Q)) position.sub(new Vector3f(0.0f, 0.0f, speed));
        if (input.isKeyDown(KeyEvent.VK_E)) position.add(new Vector3f(0.0f, 0.0f, speed));

        updateRotation(mouse);
        recalculateMatrices();
    }

    private void updateOrbit(@NotNull InputState input, float dt, @NotNull Vector2f mouse) {
        final Vector3f target = getForwardVector();
        updateRotation(mouse);
        position.add(target.sub(getForwardVector()).mul(distance));
        recalculateMatrices();
    }

    private void updatePan(@NotNull InputState input, float dt, @NotNull Vector2f mouse) {
        final float speed = (float) (Math.sqrt(distance) * dt);
        position.sub(getRightVector().mul(mouse.x * speed));
        position.add(getUpVector().mul(mouse.y * speed));
        recalculateMatrices();
    }

    public void resize(int width, int height) {
        final ModelViewerSettings settings = ModelViewerSettings.getInstance();
        windowSize.set(width, height);
        aspectRatio = (float) width / height;
        projectionMatrix.setPerspective(settings.fieldOfView * (float) Math.PI / 180.0f, aspectRatio, settings.nearClip, settings.farClip);
        recalculateMatrices();
    }

    public void lookAt(@NotNull Vector3fc target) {
        final Vector3f dir = target.sub(position, new Vector3f()).normalize();
        yaw = (float) Math.atan2(dir.y, dir.x);
        pitch = (float) Math.asin(dir.z);
        distance = position.distance(target);
        clampRotation();
        recalculateMatrices();
    }

    public void move(@NotNull Vector3fc newLocation) {
        position.set(newLocation);
        recalculateMatrices();
    }

    public void zoom(float newDistance) {
        final float delta = newDistance - distance;
        if (delta != 0.0f) {
            position.sub(getForwardVector().mul(delta));
            distance = newDistance;
            recalculateMatrices();
        }
    }

    @NotNull
    public Vector3fc getPosition() {
        return position;
    }

    @NotNull
    public Vector3f getTarget() {
        return getForwardVector().mulAdd(distance, position);
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
    public Vector3f getForwardVector() {
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

    private void recalculateMatrices() {
        viewMatrix.setLookAt(position, getForwardVector().add(position), UP);
    }

    private void updateRotation(@NotNull Vector2f delta) {
        if (delta.x != 0.0f || delta.y != 0.0f) {
            yaw -= (float) (Math.PI * delta.x / windowSize.x);
            pitch -= (float) (Math.PI / aspectRatio * delta.y / windowSize.y);
            clampRotation();
        }
    }

    private void clampRotation() {
        float limit = (float) (89.5f * Math.PI / 180.0f);
        pitch = MathUtils.clamp(pitch, -limit, limit);
    }
}
