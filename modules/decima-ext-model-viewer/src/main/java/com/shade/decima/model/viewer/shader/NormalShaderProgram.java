package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;

import java.io.IOException;

public class NormalShaderProgram extends ModelShaderProgram {
    public NormalShaderProgram() throws IOException {
        super(
            new Shader[]{
                Shader.fromResource(Shader.Type.VERTEX, "normal.vert"),
                Shader.fromResource(Shader.Type.FRAGMENT, "normal.frag"),
                Shader.fromResource(Shader.Type.GEOMETRY, "normal.geom")
            },
            new String[]{
                "in_position",
                "in_normal"
            }
        );
    }
}
