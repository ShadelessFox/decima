package com.shade.decima.model.viewer.isr.impl;

import com.shade.decima.model.viewer.Model;
import com.shade.decima.model.viewer.isr.*;
import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.decima.model.viewer.shader.RegularShaderProgram;
import com.shade.gl.Attribute;
import com.shade.gl.ShaderProgram;
import com.shade.gl.VAO;
import com.shade.util.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawElements;

public class NodeModel implements Model {
    private final Node node;
    private final Map<Primitive, VAO> vaos = new IdentityHashMap<>();
    private final Map<Primitive, Vector3fc> colors = new HashMap<>();

    public NodeModel(@NotNull Node node) {
        this.node = node;
    }

    @Override
    public void render(@NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        render(node, program, transform);
    }

    private void render(@NotNull Node node, @NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        if (!node.isVisible()) {
            return;
        }

        if (node.getMatrix() != null) {
            transform = transform.mul(node.getMatrix(), new Matrix4f());
        }

        if (program instanceof ModelShaderProgram p) {
            p.getModel().set(transform);
        }

        final Mesh mesh = node.getMesh();

        if (mesh != null) {
            for (Primitive primitive : mesh.primitives()) {
                final VAO vao = vaos.computeIfAbsent(primitive, NodeModel::createPrimitiveVao);
                final boolean softShaded;

                if (program instanceof RegularShaderProgram p) {
                    p.getColor().set(colors.computeIfAbsent(primitive, NodeModel::createPrimitiveColor));
                    softShaded = p.isSoftShaded();
                    p.setSoftShaded(softShaded && primitive.attributes().containsKey(Attribute.Semantic.NORMAL));
                } else {
                    softShaded = false;
                }

                try (VAO ignored = vao.bind()) {
                    glDrawElements(GL_TRIANGLES, primitive.indices().count(), primitive.indices().componentType().glType(), 0);
                }

                if (program instanceof RegularShaderProgram p) {
                    p.setSoftShaded(softShaded);
                }
            }
        }

        for (Node child : node.getChildren()) {
            render(child, program, transform);
        }
    }

    @Override
    public void dispose() {
        for (VAO vao : vaos.values()) {
            vao.dispose();
        }
    }

    @NotNull
    private static VAO createPrimitiveVao(@NotNull Primitive primitive) {
        class BufferData {
            private final List<Attribute> attributes = new ArrayList<>();
            private int start = Integer.MAX_VALUE;
            private int end;
        }

        final Map<Buffer, BufferData> buffers = new HashMap<>();

        for (Map.Entry<Attribute.Semantic, Accessor> attribute : primitive.attributes().entrySet()) {
            final Accessor accessor = attribute.getValue();
            final BufferView view = accessor.bufferView();
            final BufferData data = buffers.computeIfAbsent(view.buffer(), b -> new BufferData());

            data.start = Math.min(data.start, view.offset() + accessor.offset());
            data.end = Math.max(data.end, view.offset() + view.length());
            data.attributes.add(new Attribute(
                attribute.getKey(),
                accessor.componentType(),
                accessor.elementType().componentCount(),
                view.offset() + accessor.offset(),
                view.stride(),
                accessor.normalized()
            ));
        }

        final VAO vao = new VAO();

        for (Map.Entry<Buffer, BufferData> entry : buffers.entrySet()) {
            vao.createBuffer(entry.getValue().attributes).put(
                entry.getKey().data(),
                0,
                entry.getKey().length()
            );
        }

        final Accessor indices = primitive.indices();
        vao.createIndexBuffer().put(
            indices.bufferView().buffer().data(),
            indices.bufferView().offset() + indices.offset(),
            indices.bufferView().length()
        );

        return vao;
    }

    @NotNull
    private static Vector3fc createPrimitiveColor(@NotNull Primitive primitive) {
        final Random random = new Random(primitive.hash());

        return new Vector3f(
            random.nextFloat(0.5f, 1.0f),
            random.nextFloat(0.5f, 1.0f),
            random.nextFloat(0.5f, 1.0f)
        );
    }
}
