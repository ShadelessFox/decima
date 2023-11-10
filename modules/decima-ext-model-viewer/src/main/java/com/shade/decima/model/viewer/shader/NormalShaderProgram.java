package com.shade.decima.model.viewer.shader;

import com.shade.gl.Attribute;
import com.shade.gl.Shader;
import com.shade.gl.Shader.Type;

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
                "in_position", Attribute.Semantic.POSITION,
                "in_normal", Attribute.Semantic.NORMAL
            )
        );
    }
}
