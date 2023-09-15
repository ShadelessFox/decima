package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.Shader.Type;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.UniformMat4;
import com.shade.decima.model.viewer.gl.UniformVec3;
import com.shade.decima.model.viewer.isr.Primitive.Semantic;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class ViewportShaderProgram extends ShaderProgram {
    private final UniformMat4 view;
    private final UniformMat4 projection;
    private final UniformVec3 position;
    private final UniformVec3 oddColor;
    private final UniformVec3 evenColor;

    public ViewportShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("viewport.vert"), Type.VERTEX,
                Shader.fromResource("viewport.frag"), Type.FRAGMENT
            ),
            Map.of(
                "in_position", Semantic.POSITION
            )
        );

        this.view = UniformMat4.create(this, "view");
        this.projection = UniformMat4.create(this, "projection");
        this.position = UniformVec3.create(this, "viewPos");
        this.oddColor = UniformVec3.create(this, "oddColor");
        this.evenColor = UniformVec3.create(this, "evenColor");
    }

    @NotNull
    public UniformMat4 getView() {
        return view;
    }

    @NotNull
    public UniformMat4 getProjection() {
        return projection;
    }

    @NotNull
    public UniformVec3 getPosition() {
        return position;
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
