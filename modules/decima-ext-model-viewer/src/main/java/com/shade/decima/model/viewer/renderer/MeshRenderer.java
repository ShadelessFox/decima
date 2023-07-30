package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.MeshViewerCanvas;
import com.shade.decima.model.viewer.Renderer;
import com.shade.decima.model.viewer.mesh.Mesh;
import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.decima.model.viewer.shader.NormalShaderProgram;
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

    private ModelShaderProgram modelProgram;
    private NormalShaderProgram normalProgram;
    private Mesh mesh;
    private boolean loaded;

    public MeshRenderer(@NotNull Camera camera) {
        this.camera = camera;
    }

    @Override
    public void setup() throws IOException {
        modelProgram = new ModelShaderProgram();
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
            } catch (IOException e) {
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

        modelProgram.bind();
        modelProgram.getModel().set(model);
        modelProgram.getView().set(view);
        modelProgram.getProjection().set(projection);
        modelProgram.getPosition().set(camera.getPosition());

        if (canvas.isShowWireframe()) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        mesh.draw(modelProgram);

        if (canvas.isShowWireframe()) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        modelProgram.unbind();

        if (canvas.isShowNormals()) {
            normalProgram.bind();
            normalProgram.getModel().set(model);
            normalProgram.getView().set(view);
            normalProgram.getProjection().set(projection);

            mesh.draw(normalProgram);

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
