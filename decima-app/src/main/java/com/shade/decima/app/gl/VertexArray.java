package com.shade.decima.app.gl;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;

public final class VertexArray implements GLObject {
    private final int array = glGenVertexArrays();
    private final List<VertexBuffer> buffers = new ArrayList<>(1);

    /**
     * Creates a new array buffer. The returned buffer is <b>bound</b>.
     */
    public VertexBuffer createBuffer(List<VertexAttribute> attributes) {
        var buffer = createBuffer(GL_ARRAY_BUFFER);
        for (VertexAttribute attr : attributes) {
            int index = attr.location();
            glEnableVertexAttribArray(index);
            glVertexAttribPointer(index, attr.glSize(), attr.glType(), attr.normalized(), attr.stride(), attr.offset());
        }
        return buffer;
    }

    /**
     * Creates a new element array buffer. The returned buffer is <b>bound</b>.
     */
    public VertexBuffer createIndexBuffer() {
        return createBuffer(GL_ELEMENT_ARRAY_BUFFER);
    }

    @Override
    public VertexArray bind() {
        glBindVertexArray(array);
        buffers.forEach(VertexBuffer::bind);
        return this;
    }

    @Override
    public void unbind() {
        buffers.forEach(VertexBuffer::unbind);
        glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        buffers.forEach(VertexBuffer::dispose);
        glDeleteVertexArrays(array);
    }

    private VertexBuffer createBuffer(int target) {
        ensureBound();
        var buffer = new VertexBuffer(target);
        buffers.add(buffer);
        buffer.bind();
        return buffer;
    }

    private void ensureBound() {
        if (glGetInteger(GL_VERTEX_ARRAY_BINDING) != array) {
            throw new IllegalArgumentException("Vertex array is not bound");
        }
    }
}
