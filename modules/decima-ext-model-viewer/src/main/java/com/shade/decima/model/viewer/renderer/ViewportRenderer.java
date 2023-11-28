package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.Renderer;
import com.shade.decima.model.viewer.shader.ViewportShaderProgram;
import com.shade.gl.Attribute;
import com.shade.gl.VAO;
import com.shade.platform.model.Disposable;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class ViewportRenderer implements Renderer {
    private static final Attribute[] ATTRIBUTES = {
        new Attribute(Attribute.Semantic.POSITION, Attribute.ComponentType.FLOAT, 2, 0, Float.BYTES * 2, false)
    };

    private static final float[] VERTICES = {
        -1.0f, +1.0f,
        +1.0f, +1.0f,
        +1.0f, -1.0f,
        +1.0f, -1.0f,
        -1.0f, -1.0f,
        -1.0f, +1.0f
    };

    private ViewportShaderProgram program;
    private VAO vao;

    @Override
    public void setup() throws IOException {
        program = new ViewportShaderProgram();

        vao = new VAO();
        vao.createBuffer(ATTRIBUTES).put(VERTICES);
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        final Camera camera = viewport.getCamera();

        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        try (var ignored = this.vao.bind()) {
            try (var program = (ViewportShaderProgram) this.program.bind()) {
                program.getView().set(camera.getViewMatrix());
                program.getProjection().set(camera.getProjectionMatrix());
                program.getPosition().set(camera.getPosition());
                program.getOddColor().set(new Vector3f(ColorIcon.getColor(viewport.getBackground(), true).getColorComponents(null)));
                program.getEvenColor().set(new Vector3f(ColorIcon.getColor(viewport.getBackground(), false).getColorComponents(null)));

                glDrawArrays(GL_TRIANGLES, 0, VERTICES.length / 2);
            }
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    @Override
    public void dispose() {
        Disposable.dispose(program);
        Disposable.dispose(vao);

        program = null;
        vao = null;
    }
}
