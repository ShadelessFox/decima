package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.Camera;
import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.shader.ViewportShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.platform.ui.icons.ColorIcon;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class ViewportRenderer extends QuadRenderer {
    private ViewportShaderProgram program;

    @Override
    public void setup() throws IOException {
        super.setup();

        program = new ViewportShaderProgram();
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        final Camera camera = viewport.getCamera();

        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        try (var program = (ViewportShaderProgram) this.program.bind()) {
            program.getView().set(camera.getViewMatrix());
            program.getProjection().set(camera.getProjectionMatrix());
            program.getPosition().set(camera.getPosition());
            program.getOddColor().set(new Vector3f(ColorIcon.getColor(viewport.getBackground(), true).getColorComponents(null)));
            program.getEvenColor().set(new Vector3f(ColorIcon.getColor(viewport.getBackground(), false).getColorComponents(null)));

            super.update(dt, handler, viewport);
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    @Override
    public void dispose() {
        super.dispose();

        Disposable.dispose(program);
        program = null;
    }
}
