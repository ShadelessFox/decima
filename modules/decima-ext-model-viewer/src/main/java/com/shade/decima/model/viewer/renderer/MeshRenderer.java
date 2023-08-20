package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.decima.model.viewer.Renderer;
import com.shade.decima.model.viewer.mesh.Mesh;
import com.shade.decima.model.viewer.shader.NormalShaderProgram;
import com.shade.decima.model.viewer.shader.SkinnedShaderProgram;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public class MeshRenderer implements Renderer {
    private static final Logger log = LoggerFactory.getLogger(MeshRenderer.class);

    private final Camera camera;

    private SkinnedShaderProgram skinnedProgram;
    private NormalShaderProgram normalProgram;
    private Mesh mesh;
    private boolean loaded;

    public MeshRenderer(@NotNull Camera camera) {
        this.camera = camera;
    }

    @Override
    public void setup() throws IOException {
        skinnedProgram = new SkinnedShaderProgram();
        normalProgram = new NormalShaderProgram();
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull MeshViewerCanvas canvas) {
        camera.resize(canvas.getWidth(), canvas.getHeight());
        camera.update(dt, handler);

        if (mesh != null && !loaded) {
            try {
                mesh.load();
                loaded = true;
            } catch (Exception e) {
                log.error("Can't load mesh", e);
                mesh = null;
            }
        }

        if (mesh == null) {
            return;
        }

        final Matrix4f model = new Matrix4f().rotate((float) Math.toRadians(-90), 1.0f, 0.0f, 0.0f);
        final Matrix4fc view = camera.getViewMatrix();
        final Matrix4fc projection = camera.getProjectionMatrix();

        skinnedProgram.bind();
        skinnedProgram.getModel().set(model);
        skinnedProgram.getView().set(view);
        skinnedProgram.getProjection().set(projection);
        skinnedProgram.getPosition().set(camera.getPosition());
        skinnedProgram.getPosition().set(camera.getPosition());
        skinnedProgram.getFlags().set(canvas.isSoftShading() ? SkinnedShaderProgram.FLAG_SOFT_SHADING : 0);

        mesh.draw(skinnedProgram, model);

        if (canvas.isShowWireframe()) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

            skinnedProgram.getFlags().set(SkinnedShaderProgram.FLAG_WIREFRAME);
            mesh.draw(skinnedProgram, model);

            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        skinnedProgram.unbind();

        if (canvas.isShowNormals()) {
            normalProgram.bind();
            normalProgram.getModel().set(model);
            normalProgram.getView().set(view);
            normalProgram.getProjection().set(projection);

            mesh.draw(normalProgram, model);

            normalProgram.unbind();
        }
    }

    @Override
    public void dispose() {
        if (mesh != null && loaded) {
            mesh.dispose();
        }
    }

    public void setMesh(@Nullable Mesh mesh) {
        if (this.mesh != mesh) {
            if (this.mesh != null && loaded) {
                this.mesh.dispose();
                this.loaded = false;
            }

            this.mesh = mesh;
        }
    }
}
