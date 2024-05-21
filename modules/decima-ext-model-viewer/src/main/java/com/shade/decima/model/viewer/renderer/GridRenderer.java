package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.shader.GridShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public class GridRenderer extends QuadRenderer {
    private GridShaderProgram program;

    @Override
    public void setup() throws IOException {
        super.setup();

        program = new GridShaderProgram();
    }

    @Override
    public void render(float dt, @NotNull ModelViewport viewport) {
        final Camera camera = viewport.getCamera();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try (var program = (GridShaderProgram) this.program.bind()) {
            program.getView().set(camera.getViewMatrix());
            program.getProjection().set(camera.getProjectionMatrix());

            super.render(dt, viewport);
        }

        glDisable(GL_BLEND);
    }

    @Override
    public void dispose() {
        super.dispose();

        Disposable.dispose(program);
        program = null;
    }
}
