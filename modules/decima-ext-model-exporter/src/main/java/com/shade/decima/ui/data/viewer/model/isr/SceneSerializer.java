package com.shade.decima.ui.data.viewer.model.isr;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.viewer.isr.*;
import com.shade.decima.model.viewer.isr.Accessor.Target;
import com.shade.decima.model.viewer.isr.impl.StaticBuffer;
import com.shade.decima.ui.data.ValueController;
import com.shade.gl.Attribute.ComponentType;
import com.shade.gl.Attribute.ElementType;
import com.shade.gl.Attribute.Semantic;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.model.util.MathUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SceneSerializer {
    private static final Logger log = LoggerFactory.getLogger(SceneSerializer.class);

    private SceneSerializer() {
        // prevents instantiation
    }

    @NotNull
    public static Node serialize(@NotNull ProgressMonitor monitor, @NotNull ValueController<RTTIObject> controller) throws IOException {
        return serialize(monitor, controller.getValue(), controller.getCoreFile(), controller.getProject());
    }

    @NotNull
    public static Node serialize(@NotNull ProgressMonitor monitor, @NotNull RTTIObject object, @NotNull RTTICoreFile file, @NotNull Project project) throws IOException {
        final Context context = new Context();
        return serialize(monitor, object, file, project, context, null);
    }

    @Nullable
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIReference reference,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        return serialize(
            monitor,
            reference,
            file,
            project,
            context,
            reference instanceof RTTIReference.External ref ? IOUtils.getFilename(ref.path()) : null
        );
    }

    @Nullable
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIReference reference,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context,
        @Nullable String description
    ) throws IOException {
        final RTTIReference.FollowResult result = reference.follow(project, file);

        if (result == null) {
            return null;
        }

        return serialize(
            monitor,
            result.object(),
            result.file(),
            project,
            context,
            description
        );
    }

    @NotNull
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context,
        @Nullable String description
    ) throws IOException {
        final String type = object.type().getFullTypeName();

        final Node node = new Node();
        node.setName(description != null ? "%s (%s)".formatted(type, description) : type);

        try (ProgressMonitor.Task task = monitor.begin("Processing " + type, 1)) {
            switch (type) {
                // @formatter:off
                case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                    serializeRegularSkinnedMeshResource(task.split(1), node, object, file, project, context);
                case "ArtPartsDataResource" ->
                    serializeArtPartsDataResource(task.split(1), node, object, file, project, context);
                case "ArtPartsSubModelResource" ->
                    serializeArtPartsSubModelResource(task.split(1), node, object, file, project, context);
                case "ArtPartsSubModelWithChildrenResource" ->
                    serializeArtPartsSubModelWithChildrenResource(task.split(1), node, object, file, project, context);
                case "ModelPartResource" ->
                    serializeModelPartResource(task.split(1), node, object, file, project, context);
                case "LodMeshResource" ->
                    serializeLodMeshResource(task.split(1), node, object, file, project, context);
                case "LodMeshResourcePart", "MultiMeshResourcePart" ->
                    serializeMeshResourcePart(task.split(1), node, object, file, project, context);
                case "MultiMeshResource" ->
                    serializeMultiMeshResource(task.split(1), node, object, file, project, context);
                case "StaticMeshInstance" ->
                    serializeStaticMeshInstance(task.split(1), node, object, file, project, context);
                case "SkinnedModelResource" ->
                    serializeSkinnedModelResource(task.split(1), node, object, file, project, context);
                case "HumanoidBodyVariant" ->
                    serializeHumanoidBodyVariant(task.split(1), node, object, file, project, context);
                case "ObjectCollection" ->
                    serializeObjectCollection(task.split(1), node, object, file, project, context);
                case "PrefabResource" ->
                    serializePrefabResource(task.split(1), node, object, file, project, context);
                case "PrefabInstance" ->
                    serializePrefabInstance(task.split(1), node, object, file, project, context);
                case "HairModelComponentResource" ->
                    serializeHairModelComponentResource(task.split(1), node, object, file, project, context);
                case "HairResource" ->
                    serializeHairResource(task.split(1), node, object, file, project, context);
                case "HairSkinnedMeshLod" ->
                    serializeHairSkinnedMeshLod(task.split(1), node, object, file, project);
                case "HairSkinnedMesh" ->
                    serializeHairSkinnedMesh(task.split(1), node, object, file, project);
                default -> log.debug("Unhandled type: {}", type);
                // @formatter:on
            }
        }

        return node;
    }

    private static void serializeHairModelComponentResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("HairResource"), file, project, context);

        if (child != null) {
            parent.add(child);
        }
    }

    private static void serializeHairResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIObject[] lods = object.objs("MeshLods");
        final float[] distances = object.get("LODMeshDistances");

        try (ProgressMonitor.Task task = monitor.begin("Processing LODs", lods.length)) {
            for (int i = 0; i < lods.length; i++) {
                final Node child = serialize(task.split(1), lods[i], file, project, context, "#%d @ %.2f".formatted(i, distances[i]));
                child.setVisible(i == 0);

                parent.add(child);

                if (task.isCanceled()) {
                    break;
                }
            }
        }
    }

    private static void serializeHairSkinnedMeshLod(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project
    ) throws IOException {
        final RTTIObject[] meshes = object.objs("SkinnedMeshes");

        try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
            for (RTTIObject mesh : meshes) {
                parent.add(serialize(task.split(1), mesh, file, project));

                if (task.isCanceled()) {
                    break;
                }
            }
        }
    }

    private static void serializeHairSkinnedMesh(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project
    ) throws IOException {
        try (var ignored = monitor.begin("Serialize hair mesh")) {
            final var vertexArray = object.ref("SkinnedVertexArray").get(project, file).obj("Data");
            final var positionBuffer = object.ref("SkinnedPositionDataBufferResource").get(project, file).obj("Data");
            final var blendIndicesBuffer = object.ref("SkinnedBlendIndicesDataBufferResource").get(project, file).obj("Data");
            final var blendWeightsBuffer = object.ref("SkinnedBlendWeightsDataBufferResource").get(project, file).obj("Data");

            final var vertices = serializeVertexArray(vertexArray, project, null, 0).value;
            vertices.put(Semantic.POSITION, serializeDataBuffer(positionBuffer, project));
            vertices.put(Semantic.JOINTS, serializeDataBuffer(blendIndicesBuffer, project));
            vertices.put(Semantic.WEIGHTS, serializeDataBuffer(blendWeightsBuffer, project));

            final var indexArray = object.ref("SkinnedIndexArray").get(project, file).obj("Data");
            final var indices = serializeIndexArray(indexArray, project, null, null, 0).value;

            final Mesh mesh = new Mesh();
            mesh.add(new Primitive(vertices, indices, object.hashCode()));

            parent.setMesh(mesh);
        }
    }

    private static void serializePrefabInstance(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("Prefab"), file, project, context);

        if (child != null) {
            child.setMatrix(getWorldTransform(object.obj("Orientation")));
            parent.add(child);
        }
    }

    private static void serializePrefabResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("ObjectCollection"), file, project, context);

        if (child != null) {
            parent.add(child);
        }
    }

    private static void serializeObjectCollection(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] objects = object.refs("Objects");

        try (ProgressMonitor.Task task = monitor.begin("Processing objects", objects.length)) {
            for (RTTIReference obj : objects) {
                final Node child = serialize(task.split(1), obj, file, project, context);

                if (child != null) {
                    parent.add(child);
                }

                if (task.isCanceled()) {
                    break;
                }
            }
        }
    }

    private static void serializeSkinnedModelResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] parts = object.refs("ModelPartResources");

        try (ProgressMonitor.Task task = monitor.begin("Processing parts", parts.length)) {
            for (RTTIReference part : parts) {
                final Node child = serialize(task.split(1), part, file, project, context);

                if (child != null) {
                    parent.add(child);
                }
            }
        }
    }

    private static void serializeHumanoidBodyVariant(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("ModelPartResource"), file, project, context);

        if (child != null) {
            parent.add(child);
        }
    }

    private static void serializeStaticMeshInstance(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("Resource"), file, project, context);

        if (child != null) {
            child.setMatrix(getWorldTransform(object.obj("Orientation")));
            parent.add(child);
        }
    }

    private static void serializeMultiMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");
            final RTTIObject[] transforms = object.objs("Transforms");

            try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
                for (int i = 0; i < meshes.length; i++) {
                    final Node child = serialize(task.split(1), meshes[i], file, project, context);

                    if (child != null) {
                        child.setMatrix(transforms.length > 0 ? getMat34(transforms[i]) : null);
                        node.add(child);
                    }

                    if (task.isCanceled()) {
                        break;
                    }
                }
            }
        } else {
            final RTTIObject[] parts = object.objs("Parts");

            try (ProgressMonitor.Task task = monitor.begin("Processing parts", parts.length)) {
                for (RTTIObject part : parts) {
                    final Node child = serialize(task.split(1), part.ref("Mesh"), file, project, context);

                    if (child != null) {
                        child.setMatrix(getWorldTransform(part.obj("Transform")));
                        node.add(child);
                    }

                    if (task.isCanceled()) {
                        break;
                    }
                }
            }
        }
    }

    private static void serializeLodMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIObject[] meshes = object.objs("Meshes");

        try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
            for (int i = 0; i < meshes.length; i++) {
                final RTTIObject mesh = meshes[i];
                final Node child = serialize(task.split(1), mesh.ref("Mesh"), file, project, context, "#%d @ %.2f".formatted(i, mesh.f32("Distance")));

                if (child != null) {
                    child.setVisible(i == 0);
                    node.add(child);
                }

                if (task.isCanceled()) {
                    break;
                }
            }
        }
    }

    private static void serializeMeshResourcePart(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        root.add(serialize(monitor, object.ref("Mesh"), file, project, context));
    }

    private static void serializeModelPartResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        root.add(serialize(monitor, object.ref("MeshResource"), file, project, context));
    }

    private static void serializeArtPartsSubModelResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        root.add(serialize(monitor, object.ref("MeshResource"), file, project, context));

        final String helperNode = object.str("HelperNode");
        if (!helperNode.isEmpty()) {
            root.setName(helperNode);
        }

        root.setVisible(!object.bool("IsHideDefault"));
    }

    private static void serializeArtPartsSubModelWithChildrenResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] children = object.refs("Children");

        try (ProgressMonitor.Task task = monitor.begin("Processing children", children.length + 1)) {
            root.add(serialize(task.split(1), object.ref("ArtPartsSubModelPartResource"), file, project, context));

            for (RTTIReference child : children) {
                root.add(serialize(task.split(1), child, file, project, context));

                if (task.isCanceled()) {
                    break;
                }
            }
        }

        root.setVisible(!object.bool("IsHideDefault"));
    }

    private static void serializeArtPartsDataResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] parts = object.refs("SubModelPartResources");

        try (ProgressMonitor.Task task = monitor.begin("Processing parts", parts.length + 1)) {
            node.add(serialize(task.split(1), object.ref("RootModel"), file, project, context));

            for (RTTIReference child : parts) {
                node.add(serialize(task.split(1), child, file, project, context));

                if (task.isCanceled()) {
                    break;
                }
            }
        }
    }

    private static void serializeRegularSkinnedMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull RTTICoreFile file,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final String uuid = RTTIUtils.uuidToString(object.get("ObjectUUID"));

        if (context.meshes.containsKey(uuid)) {
            log.debug("Reusing existing mesh for {}", uuid);
            node.setMesh(context.meshes.get(uuid));
            return;
        }

        final Mesh mesh = new Mesh();
        final Buffer buffer = switch (project.getContainer().getType()) {
            case DS, DSDC -> getBuffer(object, project, 0);
            case HZD -> null;
        };

        int position = 0;
        int start = 0;

        for (RTTIReference ref : object.refs("Primitives")) {
            final var primitive = Objects.requireNonNull(ref.get(project, file));
            final var vertexArray = primitive.ref("VertexArray").get(project, file).obj("Data");
            final var indexArray = primitive.ref("IndexArray").get(project, file).obj("Data");
            final var startIndex = primitive.i32("StartIndex");
            final var endIndex = primitive.i32("EndIndex");

            if (startIndex > 0) {
                position = start;
            }

            start = position;

            final var vertices = serializeVertexArray(vertexArray, project, buffer, position);
            if (buffer != null) {
                position = vertices.position;
            }

            final var indices = serializeIndexArray(indexArray, project, buffer, new IndexRange(startIndex, endIndex), position);
            if (buffer != null) {
                position = indices.position;
            }

            mesh.add(new Primitive(vertices.value, indices.value, primitive.i32("Hash")));
        }

        if (buffer != null && position != buffer.length()) {
            throw new IllegalStateException("Buffer was not fully read: " + position + " / " + buffer.length());
        }

        node.setMesh(mesh);
        context.meshes.put(uuid, mesh);
    }

    @NotNull
    private static WithPosition<Map<Semantic, Accessor>> serializeVertexArray(
        @NotNull RTTIObject object,
        @NotNull Project project,
        @Nullable Buffer buffer,
        int position
    ) throws IOException {
        final var vertexStreaming = object.bool("IsStreaming");
        final var vertexCount = object.i32("VertexCount");
        final Map<Semantic, Accessor> attributes = new HashMap<>();

        for (RTTIObject stream : object.objs("Streams")) {
            final var stride = stream.i32("Stride");
            final var view = getBufferView(vertexStreaming, buffer, stream, project, position, stride * vertexCount);

            for (RTTIObject element : stream.objs("Elements")) {
                final var semantic = switch (element.str("Type")) {
                    case "Pos" -> Semantic.POSITION;
                    case "Tangent" -> Semantic.TANGENT;
                    case "Normal" -> Semantic.NORMAL;
                    case "Color" -> Semantic.COLOR;
                    case "UV0" -> Semantic.TEXTURE;
                    case "BlendIndices" -> Semantic.JOINTS;
                    case "BlendWeights" -> Semantic.WEIGHTS;
                    default -> null;
                };

                if (semantic == null) {
                    continue;
                }

                final var offset = element.i8("Offset");
                final var accessor = switch (element.str("StorageType")) {
                    // @formatter:off
                    case "UnsignedByte" -> new Accessor(view, semantic.elementType(), ComponentType.UNSIGNED_BYTE, Target.VERTEX_ARRAY, offset, vertexCount, stride, false);
                    case "UnsignedByteNormalized" -> new Accessor(view, semantic.elementType(), ComponentType.UNSIGNED_BYTE, Target.VERTEX_ARRAY, offset, vertexCount, stride, true);
                    case "UnsignedShort" -> new Accessor(view, semantic.elementType(), ComponentType.UNSIGNED_SHORT, Target.VERTEX_ARRAY, offset, vertexCount, stride, false);
                    case "UnsignedShortNormalized" -> new Accessor(view, semantic.elementType(), ComponentType.UNSIGNED_SHORT, Target.VERTEX_ARRAY, offset, vertexCount, stride, true);
                    case "SignedShort" -> new Accessor(view, semantic.elementType(), ComponentType.SHORT, Target.VERTEX_ARRAY, offset, vertexCount, stride, false);
                    case "SignedShortNormalized" -> new Accessor(view, semantic.elementType(), ComponentType.SHORT, Target.VERTEX_ARRAY, offset, vertexCount, stride, true);
                    case "HalfFloat" -> new Accessor(view, semantic.elementType(), ComponentType.HALF_FLOAT, Target.VERTEX_ARRAY, offset, vertexCount, stride, false);
                    case "Float" -> new Accessor(view, semantic.elementType(), ComponentType.FLOAT, Target.VERTEX_ARRAY, offset, vertexCount, stride, false);
                    case "X10Y10Z10W2Normalized" -> new Accessor(view, semantic.elementType(), ComponentType.INT_10_10_10_2, Target.VERTEX_ARRAY, offset, vertexCount, stride, true);
                    case "X10Y10Z10W2UNorm" -> new Accessor(view, semantic.elementType(), ComponentType.UNSIGNED_INT_10_10_10_2, Target.VERTEX_ARRAY, offset, vertexCount, stride, true);
                    default -> null;
                    // @formatter:on
                };

                if (accessor == null) {
                    continue;
                }

                attributes.put(semantic, accessor);
            }

            position += MathUtils.alignUp(stride * vertexCount, 256);
        }

        return new WithPosition<>(attributes, position);
    }

    @NotNull
    private static WithPosition<Accessor> serializeIndexArray(
        @NotNull RTTIObject object,
        @NotNull Project project,
        @Nullable Buffer buffer,
        @Nullable IndexRange indexRange,
        int position
    ) throws IOException {
        final var indexStreaming = object.bool("IsStreaming");
        final var startIndex = indexRange != null ? indexRange.start : 0;
        final var endIndex = indexRange != null ? indexRange.end : object.i32("IndexCount");
        final var indexCount = endIndex - startIndex;

        final var indexType = switch (object.str("Format")) {
            case "Index16" -> ComponentType.UNSIGNED_SHORT;
            case "Index32" -> ComponentType.UNSIGNED_INT;
            default -> throw new IllegalStateException("Unknown index format: " + object.str("Format"));
        };

        final var indexView = getBufferView(
            indexStreaming,
            buffer,
            object,
            project,
            startIndex * indexType.glSize() + position,
            indexCount * indexType.glSize()
        );

        final var indexAccessor = new Accessor(
            indexView,
            ElementType.SCALAR,
            indexType,
            Target.INDEX_ARRAY,
            0,
            indexCount,
            false
        );

        position += MathUtils.alignUp(endIndex * indexType.glSize(), 256);

        return new WithPosition<>(indexAccessor, position);
    }

    @NotNull
    private static Accessor serializeDataBuffer(
        @NotNull RTTIObject object,
        @NotNull Project project
    ) throws IOException {
        final var count = object.i32("Count");
        final var stride = object.i32("Stride");
        final var view = getBufferView(object.str("Mode").equals("Streaming"), null, object, project, 0, count * stride);

        final var format = object.str("Format").split("_", 2);
        final var elementType = switch (format[0]) {
            case "R" -> ElementType.SCALAR;
            case "RG" -> ElementType.VEC2;
            case "RGB" -> ElementType.VEC3;
            case "RGBA" -> ElementType.VEC4;
            default -> throw new IllegalArgumentException("Unsupported format: " + object.str("Format"));
        };
        return switch (format[1]) {
            case "UINT_8" ->
                new Accessor(view, elementType, ComponentType.UNSIGNED_BYTE, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "UINT_16" ->
                new Accessor(view, elementType, ComponentType.UNSIGNED_SHORT, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "UINT_32" ->
                new Accessor(view, elementType, ComponentType.UNSIGNED_INT, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "INT_8" ->
                new Accessor(view, elementType, ComponentType.BYTE, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "INT_32" ->
                new Accessor(view, elementType, ComponentType.INT, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "UNORM_8" ->
                new Accessor(view, elementType, ComponentType.UNSIGNED_BYTE, Target.VERTEX_ARRAY, 0, count, stride, true);
            case "UNORM_16" ->
                new Accessor(view, elementType, ComponentType.UNSIGNED_SHORT, Target.VERTEX_ARRAY, 0, count, stride, true);
            case "FLOAT_16" ->
                new Accessor(view, elementType, ComponentType.HALF_FLOAT, Target.VERTEX_ARRAY, 0, count, stride, false);
            case "FLOAT_32" ->
                new Accessor(view, elementType, ComponentType.FLOAT, Target.VERTEX_ARRAY, 0, count, stride, false);
            default -> throw new IllegalArgumentException("Unsupported format: " + object.str("Format"));
        };
    }

    @NotNull
    private static BufferView getBufferView(
        boolean streaming,
        @Nullable Buffer buffer,
        @NotNull RTTIObject object,
        @NotNull Project project,
        int offset,
        int length
    ) throws IOException {
        if (!streaming) {
            return new StaticBuffer(object.get("Data")).asView();
        } else if (buffer != null) {
            return buffer.asView(offset, length);
        } else {
            return getBuffer(object, project, offset).asView(0, length);
        }
    }

    @NotNull
    private static Buffer getBuffer(@NotNull RTTIObject object, @NotNull Project project, int offset) throws IOException {
        final HwDataSource dataSource = object.obj("DataSource").cast();
        return new StaticBuffer(dataSource.getData(
            project.getPackfileManager(),
            dataSource.getOffset() + offset,
            dataSource.getLength()
        ));
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

    private static class Context {
        private final Map<String, Mesh> meshes = new HashMap<>();
    }

    private record WithPosition<T>(T value, int position) {}

    private record IndexRange(int start, int end) {}
}
