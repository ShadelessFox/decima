package com.shade.decima.model.viewer.gl;

import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class VBO implements Disposable {
    private final int id;
    private final int type;
    private final int usage;

    protected VBO(int type, int usage, @Nullable Attribute[] attributes) {
        this.id = glGenBuffers();
        this.type = type;
        this.usage = usage;

        bind();

        if (attributes != null && attributes.length > 0) {
            for (int i = 0; i < attributes.length; i++) {
                final Attribute attribute = attributes[i];

                if (attribute != null) {
                    glEnableVertexAttribArray(i);
                    glVertexAttribPointer(i, attribute.glSize(), attribute.glType(), attribute.normalized(), attribute.stride(), attribute.offset());
                }
            }
        }
    }

    protected VBO(int type, int usage) {
        this(type, usage, null);
    }

    public void bind() {
        glBindBuffer(type, id);
    }

    public void unbind() {
        glBindBuffer(type, 0);
    }

    public void put(@NotNull float[] data) {
        glBufferData(type, data, usage);
    }

    public void put(@NotNull ByteBuffer data) {
        glBufferData(type, data, usage);
    }

    public void put(@NotNull ShortBuffer data) {
        glBufferData(type, data, usage);
    }

    public void put(@NotNull IntBuffer data) {
        glBufferData(type, data, usage);
    }

    public void put(@NotNull FloatBuffer data) {
        glBufferData(type, data, usage);
    }

    @Override
    public void dispose() {
        glBindBuffer(type, 0);
        glDeleteBuffers(id);
    }
}
