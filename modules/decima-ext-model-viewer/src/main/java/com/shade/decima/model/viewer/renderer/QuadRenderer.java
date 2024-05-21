package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.Renderer;
import com.shade.gl.Attribute;
import com.shade.gl.VAO;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class QuadRenderer implements Renderer {
    private static final Attribute[] ATTRIBUTES = {
        new Attribute(Attribute.Semantic.POSITION, Attribute.ComponentType.FLOAT, 2, 0, 16, false),
        new Attribute(Attribute.Semantic.TEXTURE, Attribute.ComponentType.FLOAT, 2, 8, 16, false)
    };

    private static final float[] VERTICES = {
        // position   // texture
        -1.0f, +1.0f, 0.0f, 1.0f,
        -1.0f, -1.0f, 0.0f, 0.0f,
        +1.0f, +1.0f, 1.0f, 1.0f,
        +1.0f, -1.0f, 1.0f, 0.0f,
    };

    private VAO vao;

    @Override
    public void setup() throws IOException {
        vao = new VAO();
        vao.createBuffer(ATTRIBUTES).put(VERTICES);
    }

    @Override
    public void render(float dt, @NotNull ModelViewport viewport) {
        try (VAO ignored = vao.bind()) {
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        }
    }

    @Override
    public void dispose() {
        Disposable.dispose(vao);
        vao = null;
    }
}
