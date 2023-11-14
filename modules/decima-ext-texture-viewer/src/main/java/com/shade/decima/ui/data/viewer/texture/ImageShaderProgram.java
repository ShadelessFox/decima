package com.shade.decima.ui.data.viewer.texture;

import com.shade.gl.*;
import com.shade.util.NotNull;

import java.io.IOException;
import java.util.Map;

public class ImageShaderProgram extends ShaderProgram {
    private final /* sampler2D */ UniformInt sampler;
    private final /* vec2 */ UniformVec2 viewport;
    private final /* vec2 */ UniformVec2 size;
    private final /* vec2 */ UniformVec2 location;
    private final /* float */ UniformFloat zoom;
    private final /* float */ UniformFloat gamma;
    private final /* float */ UniformFloat exposure;
    private final /* int */ UniformInt flags;

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
        this.size = UniformVec2.create(this, "u_size");
        this.location = UniformVec2.create(this, "u_location");
        this.zoom = UniformFloat.create(this, "u_zoom");
        this.gamma = UniformFloat.create(this, "u_gamma");
        this.exposure = UniformFloat.create(this, "u_exposure");
        this.flags = UniformInt.create(this, "u_flags");
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
    public UniformVec2 getSize() {
        return size;
    }

    @NotNull
    public UniformVec2 getLocation() {
        return location;
    }

    @NotNull
    public UniformFloat getZoom() {
        return zoom;
    }

    @NotNull
    public UniformFloat getGamma() {
        return gamma;
    }

    @NotNull
    public UniformFloat getExposure() {
        return exposure;
    }

    @NotNull
    public UniformInt getFlags() {
        return flags;
    }
}
