package com.shade.decima.model.exporter.isr;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.viewer.isr.*;
import com.shade.decima.model.viewer.isr.Accessor.ComponentType;
import com.shade.decima.model.viewer.isr.Accessor.ElementType;
import com.shade.decima.model.viewer.isr.Primitive.Semantic;
import com.shade.decima.ui.data.ValueController;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SceneSerializer {
    private static final Logger log = LoggerFactory.getLogger(SceneSerializer.class);

    private SceneSerializer() {
        // prevents instantiation
    }

    @NotNull
    public static Node serialize(@NotNull ValueController<RTTIObject> controller) throws IOException {
        return serialize(controller.getValue(), controller.getBinary(), controller.getProject());
    }

    @Nullable
    private static Node serialize(
        @NotNull RTTIReference reference,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        final RTTIReference.FollowResult result = reference.follow(project, binary);

        if (result != null) {
            return serialize(result.object(), result.binary(), project);
        }

        return null;
    }

    @NotNull
    private static Node serialize(
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        final RTTIClass type = object.type();
        final String name = type.getFullTypeName();

        final Node node = new Node();
        node.setName("%s (%s)".formatted(name, RTTIUtils.uuidToString(object.obj("ObjectUUID"))));

        switch (name) {
            // @formatter:off
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                serializeRegularSkinnedMeshResource(node, object, binary, project);
            case "ArtPartsDataResource" ->
                serializeArtPartsDataResource(node, object, binary, project);
            case "ArtPartsSubModelResource" ->
                serializeArtPartsSubModelResource(node, object, binary, project);
            case "ArtPartsSubModelWithChildrenResource" ->
                serializeArtPartsSubModelWithChildrenResource(node, object, binary, project);
            case "ModelPartResource" ->
                serializeModelPartResource(node, object, binary, project);
            case "LodMeshResource" ->
                serializeLodMeshResource(node, object, binary, project);
            case "MultiMeshResource" ->
                serializeMultiMeshResource(node, object, binary, project);
            default -> log.debug("Unhandled type: {}", type);
            // @formatter:on
        }

        return node;
    }

    private static void serializeMultiMeshResource(
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");
            final RTTIObject[] transforms = object.objs("Transforms");

            for (int i = 0; i < meshes.length; i++) {
                final Node child = serialize(meshes[i], binary, project);

                if (child != null) {
                    child.setMatrix(transforms.length > 0 ? getMat34(transforms[i]) : null);
                    node.add(child);
                }
            }
        } else {
            for (RTTIObject part : object.objs("Parts")) {
                final Node child = serialize(part.ref("Mesh"), binary, project);

                if (child != null) {
                    child.setMatrix(getWorldTransform(part.obj("Transform")));
                    node.add(child);
                }
            }
        }
    }

    private static void serializeLodMeshResource(
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        final RTTIObject[] meshes = object.objs("Meshes");

        for (int i = 0; i < meshes.length; i++) {
            final Node child = serialize(meshes[i].ref("Mesh"), binary, project);

            if (child != null) {
                child.setVisible(i == 0);
                node.add(child);
            }
        }
    }

    private static void serializeModelPartResource(
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        root.add(serialize(object.ref("MeshResource"), binary, project));
    }

    private static void serializeArtPartsSubModelResource(
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        root.add(serialize(object.ref("MeshResource"), binary, project));

        final String helperNode = object.str("HelperNode");
        if (!helperNode.isEmpty()) {
            root.setName(helperNode);
        }

        root.setVisible(!object.bool("IsHideDefault"));
    }

    private static void serializeArtPartsSubModelWithChildrenResource(
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        root.add(serialize(object.ref("ArtPartsSubModelPartResource"), binary, project));

        for (RTTIReference child : object.refs("Children")) {
            root.add(serialize(child, binary, project));
        }

        root.setVisible(!object.bool("IsHideDefault"));
    }

    private static void serializeArtPartsDataResource(
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        node.add(serialize(object.ref("RootModel"), binary, project));

        for (RTTIReference child : object.refs("SubModelPartResources")) {
            node.add(serialize(child, binary, project));
        }
    }

    private static void serializeRegularSkinnedMeshResource(
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project
    ) throws IOException {
        final RTTIReference[] primitives = object.refs("Primitives");

        final Mesh mesh = new Mesh();
        final Buffer buffer;

        if (project.getContainer().getType() == GameType.HZD) {
            buffer = null;
        } else {
            buffer = new Buffer(object.obj("DataSource").<HwDataSource>cast().getData(project.getPackfileManager()));
        }

        int start = 0;
        int position = 0;

        for (RTTIReference primitiveRef : primitives) {
            final RTTIObject primitive = primitiveRef.get(project, binary);
            final RTTIObject vertexArray = primitive.ref("VertexArray").get(project, binary).obj("Data");
            final RTTIObject indexArray = primitive.ref("IndexArray").get(project, binary).obj("Data");

            final boolean streamingVertices = vertexArray.bool("IsStreaming");
            final boolean streamingIndices = indexArray.bool("IsStreaming");
            final int vertexCount = vertexArray.i32("VertexCount");

            if (primitive.i32("StartIndex") > 0 || buffer == null) {
                position = start;
            }

            start = position;

            final Map<Semantic, Accessor> vertices = new LinkedHashMap<>();

            for (RTTIObject stream : vertexArray.objs("Streams")) {
                final int stride = stream.i32("Stride");
                final BufferView view = new BufferView(
                    getBuffer(streamingVertices, buffer, stream, project),
                    position,
                    stride * vertexCount,
                    stride
                );

                for (RTTIObject element : stream.objs("Elements")) {
                    final int offset = element.i8("Offset");

                    final Semantic semantic = switch (element.str("Type")) {
                        case "Pos" -> Semantic.POSITION;
                        case "Tangent" -> Semantic.TANGENT;
                        case "UV0" -> Semantic.TEXTURE;
                        case "Color" -> Semantic.COLOR;
                        case "Normal" -> Semantic.NORMAL;
                        case "BlendIndices" -> Semantic.JOINTS;
                        case "BlendWeights" -> Semantic.WEIGHTS;
                        default -> null;
                    };

                    if (semantic == null) {
                        continue;
                    }

                    final Accessor accessor = switch (element.str("StorageType")) {
                        case "UnsignedByte" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.UNSIGNED_BYTE, offset, vertexCount, false);
                        case "UnsignedByteNormalized" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.UNSIGNED_BYTE, offset, vertexCount, true);
                        case "UnsignedShort" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.UNSIGNED_SHORT, offset, vertexCount, false);
                        case "UnsignedShortNormalized" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.UNSIGNED_SHORT, offset, vertexCount, true);
                        case "SignedShort" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.SHORT, offset, vertexCount, false);
                        case "SignedShortNormalized" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.SHORT, offset, vertexCount, true);
                        case "HalfFloat" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.HALF_FLOAT, offset, vertexCount, false);
                        case "Float" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.FLOAT, offset, vertexCount, false);
                        case "X10Y10Z10W2Normalized" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.INT_10_10_10_2, offset, vertexCount, true);
                        case "X10Y10Z10W2UNorm" ->
                            new Accessor(view, semantic.getElementType(), ComponentType.UNSIGNED_INT_10_10_10_2, offset, vertexCount, true);
                        default -> null;
                    };

                    if (accessor == null) {
                        continue;
                    }

                    vertices.put(semantic, accessor);
                }

                position += IOUtils.alignUp(stride * vertexCount, 256);
            }

            final int startIndex = primitive.i32("StartIndex");
            final int endIndex = primitive.i32("EndIndex");
            final int usedIndices = endIndex - startIndex;
            final int totalIndices = indexArray.i32("IndexCount");
            final int indexSize = indexArray.str("Format").equals("Index16") ? Short.BYTES : Integer.BYTES;
            final BufferView view = new BufferView(
                getBuffer(streamingIndices, buffer, indexArray, project),
                buffer != null ? position + startIndex * indexSize : 0,
                usedIndices * indexSize,
                indexSize
            );

            final Accessor indices = new Accessor(
                view,
                ElementType.SCALAR,
                indexSize == 2 ? ComponentType.UNSIGNED_SHORT : ComponentType.UNSIGNED_INT,
                0,
                usedIndices,
                false
            );

            mesh.add(new Primitive(vertices, indices, primitive.i32("Hash")));

            position += IOUtils.alignUp(totalIndices * indexSize, 256);
        }

        if (buffer != null && position != buffer.length()) {
            throw new IllegalStateException("Buffer was not fully read");
        }

        node.setMesh(mesh);
    }

    @NotNull
    private static Buffer getBuffer(boolean streaming, @Nullable Buffer buffer, @NotNull RTTIObject object, @NotNull Project project) throws IOException {
        if (!streaming) {
            return new Buffer(object.get("Data"));
        } else if (buffer == null) {
            final HwDataSource dataSource = object.obj("DataSource").cast();
            return new Buffer(dataSource.getData(project.getPackfileManager(), dataSource.getOffset(), -1));
        } else {
            return buffer;
        }
    }

    @NotNull
    private static Matrix4f getMat34(@NotNull RTTIObject object) {
        final RTTIObject row0 = object.obj("Row0");
        final RTTIObject row1 = object.obj("Row1");
        final RTTIObject row2 = object.obj("Row2");

        return new Matrix4f(
            row0.f32("X"), row1.f32("X"), row2.f32("X"), 0.0f,
            row0.f32("Y"), row1.f32("Y"), row2.f32("Y"), 0.0f,
            row0.f32("Z"), row1.f32("Z"), row2.f32("Z"), 0.0f,
            row0.f32("W"), row1.f32("W"), row2.f32("W"), 1.0f
        );
    }

    @NotNull
    private static Matrix4f getWorldTransform(@NotNull RTTIObject object) {
        final RTTIObject col0 = object.obj("Orientation").obj("Col0");
        final RTTIObject col1 = object.obj("Orientation").obj("Col1");
        final RTTIObject col2 = object.obj("Orientation").obj("Col2");
        final RTTIObject pos = object.obj("Position");

        return new Matrix4f(
            col0.f32("X"), col0.f32("Y"), col0.f32("Z"), 0,
            col1.f32("X"), col1.f32("Y"), col1.f32("Z"), 0,
            col2.f32("X"), col2.f32("Y"), col2.f32("Z"), 0,
            (float) pos.f64("X"), (float) pos.f64("Y"), (float) pos.f64("Z"), 1
        );
    }
}
