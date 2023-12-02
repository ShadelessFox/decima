package com.shade.decima.model.viewer.renderer;

import com.shade.decima.model.viewer.InputHandler;
import com.shade.decima.model.viewer.ModelViewport;
import com.shade.decima.model.viewer.shader.OutlineShaderProgram;
import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

public class OutlineRenderer extends QuadRenderer {
    private OutlineShaderProgram outlineProgram;

    private int framebufferId;
    private int colorTexture1Id;
    private int colorTexture2Id;
    private int depthRbo;

    private int width;
    private int height;

    @Override
    public void setup() throws IOException {
        super.setup();

        // Instantiate shaders
        outlineProgram = new OutlineShaderProgram();

        // Create framebuffer color attachment
        glBindTexture(GL_TEXTURE_2D, colorTexture1Id = glGenTextures());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Create framebuffer color attachment
        glBindTexture(GL_TEXTURE_2D, colorTexture2Id = glGenTextures());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Create render buffer depth attachment
        glBindRenderbuffer(GL_RENDERBUFFER, depthRbo = glGenRenderbuffers());
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, 1, 1);

        // Create framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId = glGenFramebuffers());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture1Id, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, colorTexture2Id, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRbo);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});

        // Unbind everything
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void bind(@NotNull ModelViewport viewport) {
        if (width != viewport.getWidth() || height != viewport.getHeight()) {
            width = viewport.getWidth();
            height = viewport.getHeight();

            glBindTexture(GL_TEXTURE_2D, colorTexture1Id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glBindTexture(GL_TEXTURE_2D, colorTexture2Id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glBindRenderbuffer(GL_RENDERBUFFER, depthRbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void unbind(@NotNull ModelViewport viewport) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, viewport.getWidth(), viewport.getHeight());
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture1Id);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, colorTexture2Id);

        try (var program = (OutlineShaderProgram) outlineProgram.bind()) {
            program.diffuseSampler.set(0);
            program.maskSampler.set(1);

            super.update(dt, handler, viewport);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        Disposable.dispose(outlineProgram);
        outlineProgram = null;

        glDeleteFramebuffers(framebufferId);
        glDeleteTextures(colorTexture1Id);
        glDeleteTextures(colorTexture2Id);
        glDeleteRenderbuffers(depthRbo);
    }
}
