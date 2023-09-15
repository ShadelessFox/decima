package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.*;
import com.shade.decima.model.viewer.shader.NormalShaderProgram;
import com.shade.decima.model.viewer.shader.RegularShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public class ModelRenderer implements Renderer {
    private RegularShaderProgram regularProgram;
    private NormalShaderProgram normalProgram;
    private Model model;

    @Override
    public void setup() throws IOException {
        regularProgram = new RegularShaderProgram();
        normalProgram = new NormalShaderProgram();
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull MeshViewerCanvas canvas) {
        if (model == null) {
            return;
        }

        final Camera camera = canvas.getCamera();
        final Matrix4f modelMatrix = new Matrix4f().rotate((float) Math.toRadians(-90), 1.0f, 0.0f, 0.0f);
        final Matrix4fc viewMatrix = camera.getViewMatrix();
        final Matrix4fc projectionMatrix = camera.getProjectionMatrix();

        try (var program = (RegularShaderProgram) regularProgram.bind()) {
            program.getModel().set(modelMatrix);
            program.getView().set(viewMatrix);
            program.getProjection().set(projectionMatrix);
            program.getPosition().set(camera.getPosition());
            program.getPosition().set(camera.getPosition());
            program.getFlags().set(canvas.isSoftShading() ? RegularShaderProgram.FLAG_SOFT_SHADED : 0);

            model.render(program, modelMatrix);

            if (canvas.isShowWireframe()) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                program.setWireframe(true);

                model.render(program, modelMatrix);

                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }
        }

        if (canvas.isShowNormals()) {
            try (var program = (NormalShaderProgram) normalProgram.bind()) {
                program.bind();
                program.getModel().set(modelMatrix);
                program.getView().set(viewMatrix);
                program.getProjection().set(projectionMatrix);

                model.render(program, modelMatrix);
            }
        }
    }

    @Override
    public void dispose() {
        Disposable.dispose(regularProgram);
        Disposable.dispose(normalProgram);
        Disposable.dispose(model);

        regularProgram = null;
        normalProgram = null;
        model = null;
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
