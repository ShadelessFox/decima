package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.UniformVec3;
import com.shade.util.NotNull;

import java.io.IOException;

public class ViewportShaderProgram extends ShaderProgram {
    private final UniformVec3 oddColor;
    private final UniformVec3 evenColor;

    public ViewportShaderProgram() throws IOException {
        super(
            Shader.fromResource(Shader.Type.VERTEX, "viewport.vert"),
            Shader.fromResource(Shader.Type.FRAGMENT, "viewport.frag"),
            "in_position"
        );

        this.oddColor = UniformVec3.create(this, "oddColor");
        this.evenColor = UniformVec3.create(this, "evenColor");
    }

    @NotNull
    public UniformVec3 getOddColor() {
        return oddColor;
    }

    @NotNull
    public UniformVec3 getEvenColor() {
        return evenColor;
    }
}
