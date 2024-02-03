package com.shade.decima.ui.data.viewer.model.isr;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.viewer.isr.*;
import com.shade.decima.model.viewer.isr.Accessor.Target;
import com.shade.decima.model.viewer.isr.impl.StaticBuffer;
import com.shade.decima.ui.data.ValueController;
import com.shade.gl.Attribute;
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
        return serialize(monitor, controller.getValue(), controller.getBinary(), controller.getProject());
    }

    @NotNull
    public static Node serialize(@NotNull ProgressMonitor monitor, @NotNull RTTIObject object, @NotNull CoreBinary binary, @NotNull Project project) throws IOException {
        final Context context = new Context();
        return serialize(monitor, object, binary, project, context, null);
    }

    @Nullable
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIReference reference,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        return serialize(
            monitor,
            reference,
            binary,
            project,
            context,
            reference instanceof RTTIReference.External ref ? IOUtils.getFilename(ref.path()) : null
        );
    }

    @Nullable
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIReference reference,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context,
        @Nullable String description
    ) throws IOException {
        final RTTIReference.FollowResult result = reference.follow(project, binary);

        if (result == null) {
            return null;
        }

        return serialize(
            monitor,
            result.object(),
            result.binary(),
            project,
            context,
            description
        );
    }

    @NotNull
    private static Node serialize(
        @NotNull ProgressMonitor monitor,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
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
                    serializeRegularSkinnedMeshResource(task.split(1), node, object, binary, project, context);
                case "ArtPartsDataResource" ->
                    serializeArtPartsDataResource(task.split(1), node, object, binary, project, context);
                case "ArtPartsSubModelResource" ->
                    serializeArtPartsSubModelResource(task.split(1), node, object, binary, project, context);
                case "ArtPartsSubModelWithChildrenResource" ->
                    serializeArtPartsSubModelWithChildrenResource(task.split(1), node, object, binary, project, context);
                case "ModelPartResource" ->
                    serializeModelPartResource(task.split(1), node, object, binary, project, context);
                case "LodMeshResource" ->
                    serializeLodMeshResource(task.split(1), node, object, binary, project, context);
                case "MultiMeshResource" ->
                    serializeMultiMeshResource(task.split(1), node, object, binary, project, context);
                case "StaticMeshInstance" ->
                    serializeStaticMeshInstance(task.split(1), node, object, binary, project, context);
                case "SkinnedModelResource" ->
                    serializeSkinnedModelResource(task.split(1), node, object, binary, project, context);
                case "ObjectCollection" ->
                    serializeObjectCollection(task.split(1), node, object, binary, project, context);
                case "PrefabResource" ->
                    serializePrefabResource(task.split(1), node, object, binary, project, context);
                case "PrefabInstance" ->
                    serializePrefabInstance(task.split(1), node, object, binary, project, context);
                default -> log.debug("Unhandled type: {}", type);
                // @formatter:on
            }
        }

        return node;
    }

    private static void serializePrefabInstance(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("Prefab"), binary, project, context);

        if (child != null) {
            child.setMatrix(getWorldTransform(object.obj("Orientation")));
            parent.add(child);
        }
    }

    private static void serializePrefabResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("ObjectCollection"), binary, project, context);

        if (child != null) {
            parent.add(child);
        }
    }

    private static void serializeObjectCollection(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] objects = object.refs("Objects");

        try (ProgressMonitor.Task task = monitor.begin("Processing objects", objects.length)) {
            for (RTTIReference obj : objects) {
                final Node child = serialize(task.split(1), obj, binary, project, context);

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
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] parts = object.refs("ModelPartResources");

        try (ProgressMonitor.Task task = monitor.begin("Processing parts", parts.length)) {
            for (RTTIReference part : parts) {
                final Node child = serialize(task.split(1), part, binary, project, context);

                if (child != null) {
                    parent.add(child);
                }
            }
        }
    }

    private static void serializeStaticMeshInstance(
        @NotNull ProgressMonitor monitor,
        @NotNull Node parent,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final Node child = serialize(monitor, object.ref("Resource"), binary, project, context);

        if (child != null) {
            child.setMatrix(getWorldTransform(object.obj("Orientation")));
            parent.add(child);
        }
    }

    private static void serializeMultiMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node node,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        if (project.getContainer().getType() == GameType.DSDC) {
            final RTTIReference[] meshes = object.refs("Meshes");
            final RTTIObject[] transforms = object.objs("Transforms");

            try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
                for (int i = 0; i < meshes.length; i++) {
                    final Node child = serialize(task.split(1), meshes[i], binary, project, context);

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
                    final Node child = serialize(task.split(1), part.ref("Mesh"), binary, project, context);

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
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIObject[] meshes = object.objs("Meshes");

        try (ProgressMonitor.Task task = monitor.begin("Processing meshes", meshes.length)) {
            for (int i = 0; i < meshes.length; i++) {
                final RTTIObject mesh = meshes[i];
                final Node child = serialize(task.split(1), mesh.ref("Mesh"), binary, project, context, "#%d @ %.2f".formatted(i, mesh.f32("Distance")));

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

    private static void serializeModelPartResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        root.add(serialize(monitor, object.ref("MeshResource"), binary, project, context));
    }

    private static void serializeArtPartsSubModelResource(
        @NotNull ProgressMonitor monitor,
        @NotNull Node root,
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        root.add(serialize(monitor, object.ref("MeshResource"), binary, project, context));

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
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] children = object.refs("Children");

        try (ProgressMonitor.Task task = monitor.begin("Processing children", children.length + 1)) {
            root.add(serialize(task.split(1), object.ref("ArtPartsSubModelPartResource"), binary, project, context));

            for (RTTIReference child : children) {
                root.add(serialize(task.split(1), child, binary, project, context));

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
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @NotNull Context context
    ) throws IOException {
        final RTTIReference[] parts = object.refs("SubModelPartResources");

        try (ProgressMonitor.Task task = monitor.begin("Processing parts", parts.length + 1)) {
            node.add(serialize(task.split(1), object.ref("RootModel"), binary, project, context));

            for (RTTIReference child : parts) {
                node.add(serialize(task.split(1), child, binary, project, context));

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
        @NotNull CoreBinary binary,
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

        for (RTTIReference ref : object.refs("Primitives")) {
            final var primitive = Objects.requireNonNull(ref.get(project, binary));
            final var serialized = serializeMeshPrimitive(primitive, binary, project, buffer, position);
            mesh.primitives().add(serialized.primitive);
            position = serialized.position;
        }

        if (buffer != null && position != buffer.length()) {
            throw new IllegalStateException("Buffer was not fully read");
        }

        node.setMesh(mesh);
        context.meshes.put(uuid, mesh);
    }

    @NotNull
    private static PrimitiveWithPosition serializeMeshPrimitive(
        @NotNull RTTIObject object,
        @NotNull CoreBinary binary,
        @NotNull Project project,
        @Nullable Buffer buffer,
        int position
    ) throws IOException {
        if (buffer == null) {
            // Shared buffer is only used in DS. HZD has separate slices of buffer
            // for each stream so we don't need to track position across primitives.
            position = 0;
        }

        final var vertexArray = object.ref("VertexArray").get(project, binary).obj("Data");
        final var vertexStreaming = vertexArray.bool("IsStreaming");
        final var vertexCount = vertexArray.i32("VertexCount");
        final var attributes = new HashMap<Attribute.Semantic, Accessor>();

        for (RTTIObject stream : vertexArray.objs("Streams")) {
            final var stride = stream.i32("Stride");
            final var view = getBufferView(vertexStreaming, buffer, stream, project, position, stride * vertexCount);

            for (RTTIObject element : stream.objs("Elements")) {
                final var semantic = switch (element.str("Type")) {
                    case "Pos" -> Semantic.POSITION;
                    case "Tangent" -> Semantic.TANGENT;
                    case "Normal" -> Semantic.NORMAL;
                    case "Color" -> Semantic.COLOR;
                    // Note: Not used for now
                    // case "UV0" -> Semantic.TEXTURE;
                    // case "BlendIndices" -> Semantic.JOINTS;
                    // case "BlendWeights" -> Semantic.WEIGHTS;
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

        if (buffer == null) {
            // For the same reason as above
            position = 0;
        }

        final var indexArray = object.ref("IndexArray").get(project, binary).obj("Data");
        final var indexStreaming = indexArray.bool("IsStreaming");
        final var startIndex = object.i32("StartIndex");
        final var endIndex = object.i32("EndIndex");
        final var indexCount = endIndex - startIndex;

        final var indexType = switch (indexArray.str("Format")) {
            case "Index16" -> ComponentType.UNSIGNED_SHORT;
            case "Index32" -> ComponentType.UNSIGNED_INT;
            default -> throw new IllegalStateException("Unknown index format: " + indexArray.str("Format"));
        };

        final var indexView = getBufferView(
            indexStreaming,
            buffer,
            indexArray,
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

        return new PrimitiveWithPosition(
            new Primitive(attributes, indexAccessor, object.i32("Hash")),
            position + MathUtils.alignUp(indexCount * indexType.glSize(), 256)
        );
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
            return new StaticBuffer(object.get("Data")).asView(offset, length);
        } else if (buffer != null) {
            return buffer.asView(offset, length);
        } else {
            // HZD: All streams within the same vertex array share the same offset
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

    private record PrimitiveWithPosition(@NotNull Primitive primitive, int position) {
    }
}
