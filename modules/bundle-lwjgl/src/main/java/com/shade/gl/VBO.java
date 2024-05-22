package com.shade.gl;

import com.shade.util.NotNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class VBO implements GLObject<VBO> {
    private final int id;
    private final int type;
    private final int usage;

    protected VBO(int type, int usage, @NotNull Collection<Attribute> attributes) {
        this.id = glGenBuffers();
        this.type = type;
        this.usage = usage;

        try (var ignored = bind()) {
            for (Attribute attr : attributes) {
                if (attr != null) {
                    final int index = attr.semantic().ordinal();
                    glEnableVertexAttribArray(index);
                    glVertexAttribPointer(index, attr.glSize(), attr.glType(), attr.normalized(), attr.stride(), attr.offset());
                }
            }
        }
    }

    protected VBO(int type, int usage) {
        this(type, usage, List.of());
    }

    public void allocate(int size) {
        glBufferData(type, size, usage);
    }

    public void put(@NotNull byte[] data, int offset, int length) {
        put(BufferUtils.createByteBuffer(data.length).put(data, offset, length).flip());
    }

    public void put(@NotNull float[] data) {
        glBufferData(type, data, usage);
    }

    public void put(@NotNull ByteBuffer data) {
        if (data.isDirect()) {
            glBufferData(type, data, usage);
        } else {
            glBufferData(type, BufferUtils.createByteBuffer(data.capacity()).put(data).flip(), usage);
        }
    }

    public void put(@NotNull ShortBuffer data) {
        assert data.isDirect();
        glBufferData(type, data, usage);
    }

    public void put(@NotNull IntBuffer data) {
        assert data.isDirect();
        glBufferData(type, data, usage);
    }

    public void put(@NotNull FloatBuffer data) {
        assert data.isDirect();
        glBufferData(type, data, usage);
    }

    public void put(@NotNull FloatBuffer data, int offset) {
        assert data.isDirect();
        glBufferSubData(type, offset, data);
    }

    @NotNull
    @Override
    public VBO bind() {
        glBindBuffer(type, id);
        return this;
    }

    @Override
    public void unbind() {
        glBindBuffer(type, 0);
    }

    @Override
    public void dispose() {
        glBindBuffer(type, 0);
        glDeleteBuffers(id);
    }
}
