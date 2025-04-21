package com.shade.decima.app.viewport;

import com.shade.decima.math.Mat4;
import com.shade.decima.math.Vec3;

public final class Camera {
    private static final float PITCH_LIMIT = (float) Math.PI / 2 - 0.01f;

    private int width, height;
    private float x, y, z;
    private float fov;
    private float near, far;
    private float yaw, pitch;

    public Camera(float fov, float near, float far) {
        this.fov = fov;
        this.near = near;
        this.far = far;
    }

    public void rotate(float deltaX, float deltaY) {
        yaw -= (float) (Math.PI * deltaX / width);
        pitch -= (float) (Math.PI * deltaY / height);
        pitch = Math.clamp(pitch, -PITCH_LIMIT, PITCH_LIMIT);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void lookAt(Vec3 target) {
        Vec3 dir = target.sub(x, y, z).normalize();
        yaw = (float) Math.atan2(dir.y(), dir.x());
        pitch = (float) Math.asin(dir.z());
        pitch = Math.clamp(pitch, -PITCH_LIMIT, PITCH_LIMIT);
    }

    public Mat4 projection() {
        var aspect = (float) width / height;
        return Mat4.perspective(fov, aspect, near, far);
    }

    public Mat4 view() {
        var eye = position();
        var center = forward().add(eye);
        return Mat4.lookAt(eye, center, up());
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public void position(Vec3 position) {
        this.x = position.x();
        this.y = position.y();
        this.z = position.z();
    }

    public Vec3 up() {
        return new Vec3(0.f, 0.f, -1.f);
    }

    public Vec3 forward() {
        float yawSin = (float) Math.sin(yaw);
        float yawCos = (float) Math.cos(yaw);
        float pitchSin = (float) Math.sin(pitch);
        float pitchCos = (float) Math.cos(pitch);
        return new Vec3(yawCos * pitchCos, yawSin * pitchCos, pitchSin);
    }

    public Vec3 right() {
        float x = (float) Math.cos(yaw - Math.PI * 0.5);
        float y = (float) Math.sin(yaw - Math.PI * 0.5);
        return new Vec3(x, y, 0);
    }
}
