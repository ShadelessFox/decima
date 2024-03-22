package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.*;
import com.shade.decima.model.viewer.isr.impl.NodeModel;
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
    private static final Matrix4fc MODEL_MATRIX = new Matrix4f().rotateX((float) Math.toRadians(-90.0));

    private RegularShaderProgram regularProgram;
    private NormalShaderProgram normalProgram;
    private Model model;

    @Override
    public void setup() throws IOException {
        regularProgram = new RegularShaderProgram();
        normalProgram = new NormalShaderProgram();
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        if (model == null) {
            return;
        }

        glEnable(GL_DEPTH_TEST);

        final Camera camera = viewport.getCamera();
        final Matrix4fc viewMatrix = camera.getViewMatrix();
        final Matrix4fc projectionMatrix = camera.getProjectionMatrix();

        try (var program = (RegularShaderProgram) regularProgram.bind()) {
            program.getModel().set(MODEL_MATRIX);
            program.getView().set(viewMatrix);
            program.getProjection().set(projectionMatrix);
            program.getPosition().set(camera.getPosition());
            program.getFlags().set(viewport.isSoftShading() ? RegularShaderProgram.FLAG_SOFT_SHADED : 0);

            model.render(program, MODEL_MATRIX);

            if (viewport.isShowWireframe()) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                program.setWireframe(true);

                model.render(program, MODEL_MATRIX);

                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }
        }

        if (viewport.isShowNormals()) {
            try (var program = (NormalShaderProgram) normalProgram.bind()) {
                program.getModel().set(MODEL_MATRIX);
                program.getView().set(viewMatrix);
                program.getProjection().set(projectionMatrix);

                model.render(program, MODEL_MATRIX);
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

    @Nullable
    public Model getModel() {
        return model;
    }

    public void setModel(@Nullable Model model) {
        if (this.model != model) {
            if (this.model != null) {
                this.model.dispose();
            }

            this.model = model;
        }
    }

    public void setSelectionOnly(boolean selectionOnly) {
        if (model instanceof NodeModel m) {
            m.setSelectionOnly(selectionOnly);
        }
    }
}
