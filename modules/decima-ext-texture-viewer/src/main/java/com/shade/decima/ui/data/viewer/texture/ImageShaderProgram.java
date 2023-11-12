package com.shade.decima.ui.data.viewer.texture;

import com.shade.gl.*;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class ImageShaderProgram extends ShaderProgram {
    private final UniformInt sampler;
    private final UniformVec2 viewport;
    private final UniformVec2 mouse;

    public ImageShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("shaders/default.vert"), Shader.Type.VERTEX,
                Shader.fromResource("shaders/default.frag"), Shader.Type.FRAGMENT
            ),
            Map.of(
                "in_position", Attribute.Semantic.POSITION,
                "in_uv", Attribute.Semantic.TEXTURE
            )
        );

        this.sampler = UniformInt.create(this, "u_sampler");
        this.viewport = UniformVec2.create(this, "u_viewport");
        this.mouse = UniformVec2.create(this, "u_mouse");
    }

    @NotNull
    public UniformInt getSampler() {
        return sampler;
    }

    @NotNull
    public UniformVec2 getViewport() {
        return viewport;
    }

    @NotNull
    public UniformVec2 getMouse() {
        return mouse;
    }
}
