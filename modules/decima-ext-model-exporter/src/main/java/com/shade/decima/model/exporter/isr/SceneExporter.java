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
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SceneExporter {
    @NotNull
    public static Node export(@NotNull ValueController<RTTIObject> controller) throws IOException {
        return export(controller.getValue(), controller.getBinary(), controller.getProject());
    }

    @NotNull
    private static Node export(@NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIClass type = object.type();
        final String name = type.getFullTypeName();

        final Node node = new Node();
        node.setName("%s (%s)".formatted(name, RTTIUtils.uuidToString(object.obj("ObjectUUID"))));

        switch (name) {
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(node, object, binary, project);
            case "ArtPartsDataResource" -> exportArtPartsDataResource(node, object, binary, project);
            case "ArtPartsSubModelResource" -> exportArtPartsSubModelResource(node, object, binary, project);
            case "ArtPartsSubModelWithChildrenResource" ->
                exportArtPartsSubModelWithChildrenResource(node, object, binary, project);
            case "ModelPartResource" -> exportModelPartResource(node, object, binary, project);
            case "LodMeshResource" -> exportLodMeshResource(node, object, binary, project);
            case "MultiMeshResource" -> exportMultiMeshResource(node, object, binary, project);
            default -> System.out.println("Unhandled unsupported type " + type);
        }

        return node;
    }

    private static void exportMultiMeshResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        for (RTTIObject part : object.objs("Parts")) {
            final RTTIReference.FollowResult meshResult = part.ref("Mesh").follow(project, binary);
            final Node child = export(meshResult.object(), meshResult.binary(), project);
            child.setMatrix(getWorldTransform(part.obj("Transform")));
            node.add(child);
        }
    }

    private static void exportLodMeshResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIObject[] meshes = object.objs("Meshes");
        for (int i = 0; i < meshes.length; i++) {
            final RTTIReference.FollowResult meshResult = meshes[i].ref("Mesh").follow(project, binary);
            final Node child = export(meshResult.object(), meshResult.binary(), project);
            child.setVisible(i == 0);
            node.add(child);
        }
    }

    private static void exportModelPartResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIReference.FollowResult meshResourceResult = object.ref("MeshResource").follow(project, binary);
        node.add(export(meshResourceResult.object(), meshResourceResult.binary(), project));
    }

    private static void exportArtPartsSubModelResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIReference.FollowResult meshResourceResult = object.ref("MeshResource").follow(project, binary);
        if (meshResourceResult != null) {
            node.add(export(meshResourceResult.object(), meshResourceResult.binary(), project));
        }

        final String helperNode = object.str("HelperNode");
        if (!helperNode.isEmpty()) {
            node.setName(helperNode);
        }

        node.setVisible(!object.bool("IsHideDefault"));
    }

    private static void exportArtPartsSubModelWithChildrenResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIReference.FollowResult rootModelResult = object.ref("ArtPartsSubModelPartResource").follow(project, binary);
        if (rootModelResult != null) {
            node.add(export(rootModelResult.object(), rootModelResult.binary(), project));
        }

        for (RTTIReference child : object.refs("Children")) {
            final RTTIReference.FollowResult subModelPartResourceResult = child.follow(project, binary);
            node.add(export(subModelPartResourceResult.object(), subModelPartResourceResult.binary(), project));
        }

        node.setVisible(!object.bool("IsHideDefault"));
    }

    private static void exportArtPartsDataResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final RTTIReference.FollowResult rootModelResult = object.ref("RootModel").follow(project, binary);
        node.add(export(rootModelResult.object(), rootModelResult.binary(), project));

        for (RTTIReference subModelPartResourceRef : object.refs("SubModelPartResources")) {
            final RTTIReference.FollowResult subModelPartResourceResult = subModelPartResourceRef.follow(project, binary);
            node.add(export(subModelPartResourceResult.object(), subModelPartResourceResult.binary(), project));
        }
    }

    private static void exportRegularSkinnedMeshResource(@NotNull Node node, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
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
                final BufferView view;

                if (!streamingVertices) {
                    view = new BufferView(
                        new Buffer(stream.get("Data")),
                        position,
                        stride * vertexCount,
                        stride
                    );
                } else if (buffer == null) {
                    final HwDataSource dataSource = stream.obj("DataSource").cast();
                    view = new BufferView(
                        new Buffer(dataSource.getData(project.getPackfileManager(), -1, -1)),
                        position + dataSource.getOffset(),
                        stride * vertexCount,
                        stride
                    );
                } else {
                    view = new BufferView(
                        buffer,
                        position,
                        stride * vertexCount,
                        stride
                    );
                }

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
            final BufferView view;

            if (!streamingIndices) {
                view = new BufferView(
                    new Buffer(indexArray.get("Data")),
                    0,
                    usedIndices * indexSize,
                    indexSize
                );
            } else if (buffer == null) {
                view = new BufferView(
                    new Buffer(indexArray.obj("DataSource").<HwDataSource>cast().getData(project.getPackfileManager())),
                    0,
                    usedIndices * indexSize,
                    indexSize
                );
            } else {
                view = new BufferView(
                    buffer,
                    position + startIndex * indexSize,
                    usedIndices * indexSize,
                    indexSize
                );
            }

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

//        if (position != buffer.length()) {
//            throw new IllegalStateException("Buffer was not fully read");
//        }

        node.setMesh(mesh);
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
