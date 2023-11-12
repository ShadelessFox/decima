package com.shade.gl;

import com.shade.util.NotNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.glCompressedTexImage2D;

public class Texture implements GLObject<Texture> {
    private final int id;

    public Texture() {
        this.id = glGenTextures();
    }

    @NotNull
    public static Texture fromCompressed(@NotNull ByteBuffer data, int width, int height, int format) {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(data.remaining());
        buffer.put(data);
        buffer.position(0);

        try (Texture texture = new Texture().bind()) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glCompressedTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, buffer);

            return texture;
        }
    }

    @NotNull
    @Override
    public Texture bind() {
        glBindTexture(GL_TEXTURE_2D, id);
        return this;
    }

    @Override
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void dispose() {
        glDeleteTextures(id);
    }
}
