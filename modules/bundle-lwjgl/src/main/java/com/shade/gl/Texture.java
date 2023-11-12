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
    public static Texture from2D(@NotNull ByteBuffer data, int width, int height, int internalFormat, int format, int type) {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(data.remaining());
        buffer.put(data);
        buffer.position(0);

        try (Texture texture = new Texture().bind()) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, buffer);

            return texture;
        }
    }

    @NotNull
    public static Texture fromCompressed2D(@NotNull ByteBuffer data, int width, int height, int internalFormat) {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(data.remaining());
        buffer.put(data);
        buffer.position(0);

        try (Texture texture = new Texture().bind()) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glCompressedTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, buffer);

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
