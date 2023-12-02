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
    private int colorTextureId;
    private int maskTextureId;
    private int depthBufferId;

    private int lastWidth;
    private int lastHeight;

    @Override
    public void setup() throws IOException {
        super.setup();

        outlineProgram = new OutlineShaderProgram();

        glBindTexture(GL_TEXTURE_2D, colorTextureId = glGenTextures());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, maskTextureId = glGenTextures());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId = glGenRenderbuffers());
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, 1, 1);

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId = glGenFramebuffers());
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, maskTextureId, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferId);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    public void bind(int width, int height) {
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width;
            lastHeight = height;

            glBindTexture(GL_TEXTURE_2D, colorTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glBindTexture(GL_TEXTURE_2D, maskTextureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void update(float dt, @NotNull InputHandler handler, @NotNull ModelViewport viewport) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTextureId);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, maskTextureId);

        try (var program = (OutlineShaderProgram) outlineProgram.bind()) {
            program.colorSampler.set(0);
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
        glDeleteTextures(colorTextureId);
        glDeleteTextures(maskTextureId);
        glDeleteRenderbuffers(depthBufferId);
    }
}
