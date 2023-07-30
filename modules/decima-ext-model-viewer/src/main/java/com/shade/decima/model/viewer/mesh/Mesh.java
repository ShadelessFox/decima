package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import java.io.IOException;

public interface Mesh extends Disposable {
    void load() throws IOException;

    void draw(@NotNull ModelShaderProgram program);
}
