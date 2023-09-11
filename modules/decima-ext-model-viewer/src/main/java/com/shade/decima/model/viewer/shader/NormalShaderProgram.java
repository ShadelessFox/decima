package com.shade.decima.model.viewer.shader;

import com.shade.decima.model.viewer.gl.Shader;
import com.shade.decima.model.viewer.gl.Shader.Type;
import com.shade.decima.model.viewer.isr.Primitive.Semantic;

import java.io.IOException;
import java.util.Map;

public class NormalShaderProgram extends ModelShaderProgram {
    public NormalShaderProgram() throws IOException {
        super(
            Map.of(
                Shader.fromResource("normal.vert"), Type.VERTEX,
                Shader.fromResource("normal.frag"), Type.FRAGMENT,
                Shader.fromResource("normal.geom"), Type.GEOMETRY
            ),
            Map.of(
                "in_position", Semantic.POSITION,
                "in_normal", Semantic.NORMAL
            )
        );
    }
}
