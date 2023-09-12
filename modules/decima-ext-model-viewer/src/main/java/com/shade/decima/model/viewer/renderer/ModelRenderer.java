package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.*;
import com.shade.decima.model.viewer.shader.NormalShaderProgram;
import com.shade.decima.model.viewer.shader.RegularShaderProgram;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public class ModelRenderer implements Renderer {
    private final Camera camera;

    private RegularShaderProgram regularProgram;
    private NormalShaderProgram normalProgram;
    private Model model;

    public ModelRenderer(@NotNull Camera camera) {
        this.camera = camera;
    }

    @Override
    public void setup() throws IOException {
        regularProgram = new RegularShaderProgram();
        normalProgram = new NormalShaderProgram();
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull MeshViewerCanvas canvas) {
        camera.resize(canvas.getWidth(), canvas.getHeight());
        camera.update(dt, handler);

        if (model == null) {
            return;
        }

        final Matrix4f modelMatrix = new Matrix4f().rotate((float) Math.toRadians(-90), 1.0f, 0.0f, 0.0f);
        final Matrix4fc viewMatrix = camera.getViewMatrix();
        final Matrix4fc projectionMatrix = camera.getProjectionMatrix();

        regularProgram.bind();
        regularProgram.getModel().set(modelMatrix);
        regularProgram.getView().set(viewMatrix);
        regularProgram.getProjection().set(projectionMatrix);
        regularProgram.getPosition().set(camera.getPosition());
        regularProgram.getPosition().set(camera.getPosition());
        regularProgram.getFlags().set(canvas.isSoftShading() ? RegularShaderProgram.FLAG_SOFT_SHADED : 0);

        model.render(regularProgram, modelMatrix);

        if (canvas.isShowWireframe()) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            regularProgram.setWireframe(true);

            model.render(regularProgram, modelMatrix);

            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        regularProgram.unbind();

        if (canvas.isShowNormals()) {
            normalProgram.bind();
            normalProgram.getModel().set(modelMatrix);
            normalProgram.getView().set(viewMatrix);
            normalProgram.getProjection().set(projectionMatrix);

            model.render(normalProgram, modelMatrix);

            normalProgram.unbind();
        }
    }

    @Override
    public void dispose() {
        if (model != null) {
            model.dispose();
            model = null;
        }
    }

    public void setModel(@Nullable Model model) {
        if (this.model != model) {
            if (this.model != null) {
                this.model.dispose();
            }

            this.model = model;
        }
    }
}
