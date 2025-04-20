package com.shade.decima.model.viewer;

import com.shade.gl.ShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import org.joml.Matrix4fc;

public interface Model extends Disposable {
    void render(@NotNull ShaderProgram program, @NotNull Matrix4fc transform);
}
