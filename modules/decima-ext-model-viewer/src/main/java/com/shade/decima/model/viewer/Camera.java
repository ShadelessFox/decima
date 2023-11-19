package com.shade.decima.model.viewer;

import com.shade.util.NotNull;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

public interface Camera {
    void update(float dt, @NotNull InputHandler input);

    void resize(int width, int height);

    @NotNull
    Vector3fc getPosition();

    void setPosition(@NotNull Vector3fc position);

    @NotNull
    Matrix4fc getViewMatrix();

    @NotNull
    Matrix4fc getProjectionMatrix();
}
