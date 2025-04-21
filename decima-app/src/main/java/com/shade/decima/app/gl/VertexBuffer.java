package com.shade.decima.app.gl;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;

public final class VertexBuffer implements GLObject {
    private final int buffer = glGenBuffers();
    private final int target;

    VertexBuffer(int target) {
        this.target = target;
    }

    public void put(ByteBuffer data, int usage) {
        ensureBound();
        if (data.isDirect()) {
            glBufferData(target, data, usage);
            return;
        }
        glBufferData(target, BufferUtils.createByteBuffer(data.remaining()).put(data).flip(), usage);
    }

    public void put(IntBuffer data, int usage) {
        ensureBound();
        if (data.isDirect()) {
            glBufferData(target, data, usage);
            return;
        }
        glBufferData(target, BufferUtils.createIntBuffer(data.remaining()).put(data).flip(), usage);
    }

    public void put(FloatBuffer data, int usage) {
        ensureBound();
        if (data.isDirect()) {
            glBufferData(target, data, usage);
            return;
        }
        glBufferData(target, BufferUtils.createFloatBuffer(data.remaining()).put(data).flip(), usage);
    }

    @Override
    public VertexBuffer bind() {
        glBindBuffer(target, buffer);
        return this;
    }

    @Override
    public void unbind() {
        glBindBuffer(target, 0);
    }

    @Override
    public void dispose() {
        glDeleteBuffers(buffer);
    }

    private void ensureBound() {
        var name = switch (target) {
            case GL_ARRAY_BUFFER -> GL_ARRAY_BUFFER_BINDING;
            case GL_ELEMENT_ARRAY_BUFFER -> GL_ELEMENT_ARRAY_BUFFER_BINDING;
            default -> throw new IllegalArgumentException("Unknown buffer target: " + target);
        };
        if (glGetInteger(name) != buffer) {
            throw new IllegalArgumentException("Vertex buffer is not bound");
        }
    }
}
