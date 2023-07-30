package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.Renderer;
import com.shade.decima.model.viewer.gl.Attribute;
import com.shade.decima.model.viewer.gl.VAO;
import com.shade.decima.model.viewer.shader.ViewportShaderProgram;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public class ViewportRenderer implements Renderer {
    private static final Attribute[] ATTRIBUTES = {
        new Attribute(Attribute.Type.FLOAT, 2, 0, Float.BYTES * 2, false)
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
    public void update(float dt, @NotNull InputHandler handler, @NotNull Canvas canvas) {
        program.bind();
        program.getOddColor().set(new Vector3f(ColorIcon.getColor(canvas.getBackground(), true).getColorComponents(null)));
        program.getEvenColor().set(new Vector3f(ColorIcon.getColor(canvas.getBackground(), false).getColorComponents(null)));
        vao.bind();

        glDisable(GL_DEPTH_TEST);
        glDrawArrays(GL_TRIANGLES, 0, VERTICES.length / 2);
        glEnable(GL_DEPTH_TEST);

        vao.unbind();
        program.unbind();
    }

    @Override
    public void dispose() {
        program.dispose();
        vao.dispose();
    }
}
