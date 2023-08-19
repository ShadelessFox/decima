package com.shade.decima.model.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.viewer.gl.Attribute;
import com.shade.decima.model.viewer.gl.ShaderProgram;
import com.shade.decima.model.viewer.gl.VAO;
import com.shade.decima.model.viewer.gl.VBO;
import com.shade.decima.model.viewer.shader.ModelShaderProgram;
import com.shade.decima.model.viewer.shader.SkinnedShaderProgram;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.opengl.GL15.*;

public class DecimaSkinnedMesh implements Mesh {
    private final ValueController<RTTIObject> controller;

    private Primitive[] primitives;

    public DecimaSkinnedMesh(@NotNull ValueController<RTTIObject> controller) {
        this.controller = controller;
    }

    @Override
    public void load() throws IOException {
        final RTTIObject object = controller.getValue();
        final CoreBinary binary = controller.getBinary();
        final Project project = controller.getProject();

        final HwDataSource dataSource = object.obj("DataSource").cast();
        final ByteBuffer buffer = BufferUtils.createByteBuffer(dataSource.getLength() - dataSource.getOffset());
        buffer.put(dataSource.getData(project.getPackfileManager()));
        buffer.position(0);

        final RTTIReference[] primitives = object.get("Primitives");

        this.primitives = new Primitive[primitives.length];

        int start = 0;
        int position = 0;

        for (int i = 0; i < primitives.length; i++) {
            final RTTIObject primitive = primitives[i].get(project, binary);
            final RTTIObject vertexArray = primitive.ref("VertexArray").get(project, binary).obj("Data");
            final RTTIObject indexArray = primitive.ref("IndexArray").get(project, binary).obj("Data");
            final int vertexCount = vertexArray.i32("VertexCount");
            final int indexSize = indexArray.str("Format").equals("Index16") ? Short.BYTES : Integer.BYTES;

            if (primitive.i32("StartIndex") > 0) {
                // TODO: Instead of creating VBOs storing the same data, we can share them between VAOs
                position = start;
            }

            start = position;

            enum UsageType {
                POSITION(3), NORMAL(3), BLEND_INDICES(4), BLEND_WEIGHTS(4);

                private final int components;

                UsageType(int components) {
                    this.components = components;
                }
            }

            final Map<UsageType, Attribute> attributes = new HashMap<>();

            for (RTTIObject stream : vertexArray.objs("Streams")) {
                final int stride = stream.i32("Stride");

                for (RTTIObject element : stream.objs("Elements")) {
                    final int offset = position + element.i8("Offset");

                    final UsageType usage = switch (element.str("Type")) {
                        case "Pos" -> UsageType.POSITION;
                        case "Normal" -> UsageType.NORMAL;
                        case "BlendIndices" -> UsageType.BLEND_INDICES;
                        case "BlendWeights" -> UsageType.BLEND_WEIGHTS;
                        default -> null;
                    };

                    if (usage == null) {
                        continue;
                    }

                    final Attribute attribute = switch (element.str("StorageType")) {
                        case "UnsignedByte" -> new Attribute(Attribute.Type.UNSIGNED_BYTE, usage.components, offset, stride, false);
                        case "UnsignedByteNormalized" -> new Attribute(Attribute.Type.UNSIGNED_BYTE, usage.components, offset, stride, true);
                        case "UnsignedShort" -> new Attribute(Attribute.Type.UNSIGNED_SHORT, usage.components, offset, stride, false);
                        case "UnsignedShortNormalized" -> new Attribute(Attribute.Type.UNSIGNED_SHORT, usage.components, offset, stride, true);
                        case "SignedShort" -> new Attribute(Attribute.Type.SHORT, usage.components, offset, stride, false);
                        case "SignedShortNormalized" -> new Attribute(Attribute.Type.SHORT, usage.components, offset, stride, true);
                        case "HalfFloat" -> new Attribute(Attribute.Type.HALF_FLOAT, usage.components, offset, stride, false);
                        case "Float" -> new Attribute(Attribute.Type.FLOAT, usage.components, offset, stride, false);
                        case "X10Y10Z10W2Normalized" -> new Attribute(Attribute.Type.INT_10_10_10_2, usage.components, offset, stride, true);
                        case "X10Y10Z10W2UNorm" -> new Attribute(Attribute.Type.UNSIGNED_INT_10_10_10_2, usage.components, offset, stride, true);
                        default -> null;
                    };

                    if (attribute == null) {
                        continue;
                    }

                    attributes.put(usage, attribute);
                }

                position += IOUtils.alignUp(stride * vertexCount, 256);
            }

            final int startIndex = primitive.i32("StartIndex");
            final int endIndex = primitive.i32("EndIndex");
            final int usedIndices = endIndex - startIndex;
            final int totalIndices = indexArray.i32("IndexCount");

            final VAO vao = new VAO();

            final VBO indices = vao.createIndexBuffer();
            indices.put(buffer.slice(position + startIndex * indexSize, usedIndices * indexSize));

            final VBO vertices = vao.createBuffer(attributes.get(UsageType.POSITION), attributes.get(UsageType.NORMAL), attributes.get(UsageType.BLEND_INDICES), attributes.get(UsageType.BLEND_WEIGHTS));
            vertices.put(buffer.slice(0, position));

            vao.unbind();

            final Random random = new Random(primitive.i32("Hash"));

            this.primitives[i] = new Primitive(
                vao,
                new Vector3f(random.nextFloat(0.5f, 1.0f), random.nextFloat(0.5f, 1.0f), random.nextFloat(0.5f, 1.0f)),
                usedIndices
            );

            position += IOUtils.alignUp(totalIndices * indexSize, 256);
        }

        if (position != buffer.limit()) {
            throw new IllegalStateException("Buffer was not fully read");
        }
    }

    @Override
    public void draw(@NotNull ShaderProgram program, @NotNull Matrix4fc transform) {
        if (program instanceof ModelShaderProgram p) {
            p.getModel().set(transform);
        }

        for (Primitive primitive : primitives) {
            primitive.draw(program);
        }
    }

    @Override
    public void dispose() {
        for (Primitive primitive : primitives) {
            primitive.vao.dispose();
        }
    }

    private record Primitive(@NotNull VAO vao, @NotNull Vector3fc color, int indices) {
        public void draw(@NotNull ShaderProgram program) {
            if (program instanceof SkinnedShaderProgram p) {
                p.getColor().set(color);
            }

            vao.bind();
            glDrawElements(GL_TRIANGLES, indices, GL_UNSIGNED_SHORT, 0);
            vao.unbind();
        }
    }
}
