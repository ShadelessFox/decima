package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.shader.OutlineShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import org.joml.Vector2f;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

public class OutlineRenderer extends QuadRenderer {
    private OutlineShaderProgram program;

    private int framebufferId;
    private int colorTextureId;
    private int depthTextureId;

    private int width;
    private int height;

    @Override
    public void setup() throws IOException {
        super.setup();

        // Instantiate shader
        program = new OutlineShaderProgram();

        // Create framebuffer color attachment
        colorTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Create framebuffer depth attachment
        depthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, 1, 1, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);

        // Create framebuffer
        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);

        // Unbind everything
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(@NotNull ModelViewport viewport) {
        if (width != viewport.getWidth() || height != viewport.getHeight()) {
            width = viewport.getWidth();
            height = viewport.getHeight();

            glBindTexture(GL_TEXTURE_2D, colorTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glBindTexture(GL_TEXTURE_2D, depthTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void unbind(@NotNull ModelViewport viewport) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, viewport.getWidth(), viewport.getHeight());
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTextureId);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTextureId);

        try (var program = (OutlineShaderProgram) this.program.bind()) {
            program.getSize().set(new Vector2f(width, height));

            super.update(dt, handler, viewport);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        Disposable.dispose(program);
        program = null;

        glDeleteFramebuffers(framebufferId);
        glDeleteTextures(colorTextureId);
        glDeleteTextures(depthTextureId);
    }
}
