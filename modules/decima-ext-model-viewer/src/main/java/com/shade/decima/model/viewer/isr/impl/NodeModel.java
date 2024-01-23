package com.shade.decima.model.viewer.isr.impl;

import com.shade.decima.model.viewer.Model;
import com.shade.decima.model.viewer.ModelViewport;
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

import static org.lwjgl.opengl.GL11.*;

public class NodeModel implements Model {
    private final Node root;
    private final ModelViewport viewport;
    private final Map<Node, NodeInfo> infos = new HashMap<>();
    private boolean selectionOnly;

    public NodeModel(@NotNull Node root, @NotNull ModelViewport viewport) {
        this.root = root;
        this.viewport = viewport;
    }

    @Override
    public void render(@NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        render(root, program, transform, false);
    }

    private void render(@NotNull Node node, @NotNull ShaderProgram program, @NotNull Matrix4fc transform, boolean selected) {
        if (!node.isVisible()) {
            return;
        }

        final Matrix4fc matrix = node.getMatrix();
        final Mesh mesh = node.getMesh();
        final List<Node> children = node.getChildren();

        if (matrix == null && mesh == null && children.isEmpty()) {
            return;
        }

        selected |= viewport.isShowOutlineFor(node);

        NodeInfo info = infos.get(node);

        if (info == null) {
            info = createNodeInfo(node, transform);
            infos.put(node, info);
        }

        if (selectionOnly == selected) {
            for (PrimitiveInfo primitive : info.primitives) {
                final int flags;

                if (program instanceof ModelShaderProgram p) {
                    p.getModel().set(transform);
                }

                if (program instanceof RegularShaderProgram p) {
                    flags = p.getFlags().get();

                    p.getColor().set(primitive.color);
                    p.setSoftShaded(p.isSoftShaded() && primitive.primitive.attributes().containsKey(Attribute.Semantic.NORMAL));
                    p.setSelected(selected);
                } else {
                    flags = 0;
                }

                try (VAO ignored = primitive.vao.bind()) {
                    glDrawElements(
                        GL_TRIANGLES,
                        primitive.primitive.indices().count(),
                        primitive.primitive.indices().componentType().glType(),
                        0
                    );
                }

                if (program instanceof RegularShaderProgram p) {
                    p.getFlags().set(flags);
                }
            }
        }

        // Use indexed loop to avoid allocating iterator objects
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < children.size(); i++) {
            render(children.get(i), program, info.transform, selected);
        }
    }

    @Override
    public void dispose() {
        for (NodeInfo info : infos.values()) {
            for (PrimitiveInfo primitive : info.primitives) {
                primitive.vao.dispose();
            }
        }

        infos.clear();
    }

    @NotNull
    public Node getRoot() {
        return root;
    }

    public void setSelectionOnly(boolean selectionOnly) {
        this.selectionOnly = selectionOnly;
    }

    @NotNull
    private static NodeInfo createNodeInfo(@NotNull Node node, @NotNull Matrix4fc transform) {
        final List<PrimitiveInfo> primitives = new ArrayList<>();

        final Mesh mesh = node.getMesh();
        final Matrix4fc matrix = node.getMatrix();

        if (mesh != null) {
            for (Primitive primitive : mesh.primitives()) {
                primitives.add(new PrimitiveInfo(
                    primitive,
                    createPrimitiveVao(primitive),
                    createPrimitiveColor(primitive)
                ));
            }
        }

        if (matrix != null) {
            transform = transform.mul(node.getMatrix(), new Matrix4f());
        }

        return new NodeInfo(primitives.toArray(PrimitiveInfo[]::new), transform);
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
                accessor.componentCount(),
                accessor.offset() + view.offset(),
                accessor.stride(),
                accessor.normalized()
            ));
        }

        final VAO vao = new VAO();

        for (Map.Entry<Buffer, BufferData> entry : buffers.entrySet()) {
            vao.createBuffer(entry.getValue().attributes).put(entry.getKey().asByteBuffer());
        }

        final Accessor indices = primitive.indices();
        vao.createIndexBuffer().put(indices.bufferView().asByteBuffer());

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

    private record PrimitiveInfo(@NotNull Primitive primitive, @NotNull VAO vao, @NotNull Vector3fc color) {
    }

    private record NodeInfo(@NotNull PrimitiveInfo[] primitives, @NotNull Matrix4fc transform) {
    }
}
