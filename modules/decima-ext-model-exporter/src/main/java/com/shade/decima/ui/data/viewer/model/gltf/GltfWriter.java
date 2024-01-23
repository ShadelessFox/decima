package com.shade.decima.ui.data.viewer.model.gltf;

import com.google.gson.stream.JsonWriter;
import com.shade.decima.BuildConfig;
import com.shade.decima.model.viewer.isr.*;
import com.shade.decima.model.viewer.isr.impl.DynamicBuffer;
import com.shade.gl.Attribute;
import com.shade.gl.Attribute.ComponentType;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GltfWriter {
    enum BinaryChunkType {
        JSON(0x4E4F534A, 0x20),
        BIN(0x004E4942, 0x00);

        private final int id;
        private final byte filler;

        BinaryChunkType(int id, int filler) {
            this.id = id;
            this.filler = (byte) filler;
        }
    }

    public static void writeBinary(@NotNull ProgressMonitor monitor, @NotNull Node node, @NotNull SeekableByteChannel channel) throws IOException {
        try (var task = monitor.begin("Serializing scene", 2)) {
            final CompactingNodeVisitor visitor = new CompactingNodeVisitor(task);
            final Context context = new Context();

            try (var ignored = task.split(1).begin("Compacting scene")) {
                final Node transformed = node.apply(visitor);

                if (transformed != null) {
                    context.addScene(new Scene(List.of(transformed)));
                }
            }

            try (var ignored = task.split(1).begin("Writing scene")) {
                final StringWriter json = new StringWriter();
                write(context, new JsonWriter(json), false);

                final ByteBuffer jsonBuffer = ByteBuffer.wrap(json.toString().getBytes(StandardCharsets.UTF_8));
                final ByteBuffer binaryBuffer = visitor.buffer.asByteBuffer();

                writeBinaryHeader(0, channel);
                writeBinaryChunk(BinaryChunkType.JSON, jsonBuffer, channel);
                writeBinaryChunk(BinaryChunkType.BIN, binaryBuffer, channel);
                writeBinaryHeader(Math.toIntExact(channel.position()), channel.position(0));
            }
        }
    }

    public static void writeText(@NotNull ProgressMonitor monitor, @NotNull Node node, @NotNull JsonWriter writer) throws IOException {
        try (var task = monitor.begin("Serializing scene", 2)) {
            final CompactingNodeVisitor visitor = new CompactingNodeVisitor(task);
            final Context context = new Context();

            try (var ignored = task.split(1).begin("Compacting scene")) {
                final Node transformed = node.apply(visitor);

                if (transformed != null) {
                    context.addScene(new Scene(List.of(transformed)));
                }
            }

            try (var ignored = task.split(1).begin("Writing scene")) {
                write(context, writer, true);
            }
        }
    }

    private static void writeBinaryHeader(int length, @NotNull WritableByteChannel channel) throws IOException {
        channel.write(ByteBuffer
            .allocate(12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x46546C67)
            .putInt(2)
            .putInt(length)
            .flip());
    }

    private static void writeBinaryChunk(@NotNull BinaryChunkType type, @NotNull ByteBuffer buffer, @NotNull WritableByteChannel channel) throws IOException {
        final int length = buffer.remaining();
        final int padding = IOUtils.alignUp(length, 4) - length;

        channel.write(ByteBuffer
            .allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(length + padding)
            .putInt(type.id)
            .flip());

        channel.write(buffer);

        if (padding > 0) {
            final byte[] bytes = new byte[padding];
            Arrays.fill(bytes, type.filler);
            channel.write(ByteBuffer.wrap(bytes));
        }
    }

    private static void write(@NotNull Context context, @NotNull JsonWriter writer, boolean embedBufferData) throws IOException {
        writer.beginObject();

        {
            writer.name("asset").beginObject();
            writer.name("generator").value("%s %s (%s)".formatted(BuildConfig.APP_TITLE, BuildConfig.APP_VERSION, BuildConfig.BUILD_COMMIT));
            writer.name("version").value("2.0");
            writer.endObject();
        }

        {
            writer.name("scenes").beginArray();

            for (Scene scene : context.scenes) {
                writeScene(scene, context, writer);
            }

            writer.endArray();
        }

        {
            writer.name("nodes").beginArray();

            // Can't use iterator because of co-modifications
            for (int i = 0; i < context.nodes.size(); i++) {
                writeNode(context.nodes.get(i), context, writer);
            }

            writer.endArray();
        }

        {
            writer.name("meshes").beginArray();

            for (Mesh mesh : context.meshes) {
                writeMesh(mesh, context, writer);
            }

            writer.endArray();
        }

        {
            writer.name("accessors").beginArray();

            for (Accessor accessor : context.accessors) {
                writeAccessor(accessor, context, writer);
            }

            writer.endArray();
        }

        {
            writer.name("bufferViews").beginArray();

            for (BufferViewInfo info : context.bufferViews) {
                writeBufferView(info.view, context, writer, info.accessor);
            }

            writer.endArray();
        }

        {
            writer.name("buffers").beginArray();

            for (Buffer buffer : context.buffers) {
                writeBuffer(buffer, context, writer, embedBufferData);
            }

            writer.endArray();
        }

        writer.endObject();
    }

    private static void writeScene(@NotNull Scene scene, @NotNull Context context, @NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        {
            writer.name("nodes").beginArray();

            for (Node node : scene.nodes()) {
                writer.value(context.addNode(node));
            }

            writer.endArray();
        }

        writer.endObject();
    }

    private static void writeNode(@NotNull Node node, @NotNull Context context, @NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        if (node.getName() != null) {
            writer.name("name").value(node.getName());
        }

        if (node.getMatrix() != null) {
            writer.name("matrix").beginArray();
            for (int i = 0; i < 16; i++) {
                writer.value(node.getMatrix().get(i / 4, i % 4));
            }
            writer.endArray();
        }

        if (node.getMesh() != null) {
            writer.name("mesh").value(context.addMesh(node.getMesh()));
        }

        if (!node.getChildren().isEmpty()) {
            writer.name("children").beginArray();

            for (Node child : node.getChildren()) {
                if (!child.isVisible()) {
                    continue;
                }

                writer.value(context.addNode(child));
            }

            writer.endArray();
        }

        writer.endObject();
    }

    private static void writeMesh(@NotNull Mesh mesh, @NotNull Context context, @NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        {
            writer.name("primitives").beginArray();

            for (Primitive primitive : mesh.primitives()) {
                writer.beginObject();

                writer.name("attributes").beginObject();
                for (Map.Entry<Attribute.Semantic, Accessor> entry : primitive.attributes().entrySet()) {
                    writer.name(getSemanticName(entry.getKey())).value(context.addAccessor(entry.getValue()));
                }
                writer.endObject();

                writer.name("indices").value(context.addAccessor(primitive.indices()));

                writer.endObject();
            }

            writer.endArray();
        }

        writer.endObject();
    }

    private static void writeAccessor(@NotNull Accessor accessor, @NotNull Context context, @NotNull JsonWriter writer) throws IOException {
        writer.beginObject();

        writer.name("bufferView").value(context.addBufferView(new BufferViewInfo(accessor.bufferView(), accessor)));
        writer.name("byteOffset").value(accessor.offset());
        writer.name("componentType").value(accessor.componentType().glType());
        writer.name("type").value(accessor.elementType().name());
        writer.name("count").value(accessor.count());

        if (accessor.normalized()) {
            writer.name("normalized").value(true);
        }

        if (accessor.componentType() == ComponentType.FLOAT) {
            final Accessor.FloatView view = accessor.asFloatView();
            final float[] min = new float[accessor.componentCount()];
            final float[] max = new float[accessor.componentCount()];

            Arrays.fill(min, Float.POSITIVE_INFINITY);
            Arrays.fill(max, Float.NEGATIVE_INFINITY);

            for (int i = 0; i < accessor.count(); i++) {
                for (int j = 0; j < accessor.componentCount(); j++) {
                    final float value = view.get(i, j);
                    min[j] = Math.min(min[j], value);
                    max[j] = Math.max(max[j], value);
                }
            }

            writer.name("min").beginArray();
            for (float value : min) {
                writer.value(value);
            }
            writer.endArray();

            writer.name("max").beginArray();
            for (float value : max) {
                writer.value(value);
            }
            writer.endArray();
        }

        writer.endObject();
    }

    private static void writeBufferView(@NotNull BufferView view, @NotNull Context context, @NotNull JsonWriter writer, @NotNull Accessor accessor) throws IOException {
        writer.beginObject();

        writer.name("buffer").value(context.addBuffer(view.buffer()));
        writer.name("byteOffset").value(view.offset());
        writer.name("byteLength").value(view.length());
        if (accessor.stride() != accessor.size()) {
            writer.name("byteStride").value(accessor.stride());
        }
        writer.name("target").value(accessor.target().glTarget());

        writer.endObject();
    }

    private static void writeBuffer(@NotNull Buffer buffer, @NotNull Context context, @NotNull JsonWriter writer, boolean embedData) throws IOException {
        writer.beginObject();

        if (embedData) {
            final byte[] data = new byte[buffer.length()];
            buffer.asByteBuffer().get(data);
            writer.name("uri").value("data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(data));
        }

        writer.name("byteLength").value(buffer.length());
        writer.endObject();
    }

    @NotNull
    private static String getSemanticName(@NotNull Attribute.Semantic semantic) {
        return switch (semantic) {
            case POSITION -> "POSITION";
            case NORMAL -> "NORMAL";
            case TANGENT -> "TANGENT";
            case TEXTURE -> "TEXCOORD_0";
            case COLOR -> "COLOR_0";
            case JOINTS -> "JOINTS_0";
            case WEIGHTS -> "WEIGHTS_0";
        };
    }

    private static class CompactingNodeVisitor implements NodeVisitor<Node> {
        private final DynamicBuffer buffer = new DynamicBuffer(0);
        private final ProgressMonitor.IndeterminateTask task;

        public CompactingNodeVisitor(@NotNull ProgressMonitor.IndeterminateTask task) {
            this.task = task;
        }

        @Nullable
        @Override
        public Node visit(@NotNull Node node) {
            final List<Node> children = node.getChildren().stream()
                .map(child -> child.apply(this))
                .filter(Objects::nonNull)
                .toList();

            if (task.isCanceled()) {
                return null;
            }

            Mesh mesh = node.getMesh();
            if (mesh != null) {
                mesh = compactMesh(mesh);
            }

            if (children.isEmpty() && mesh == null) {
                return null;
            }

            final Node copy = new Node();
            copy.setMatrix(node.getMatrix());
            copy.setMesh(mesh);
            copy.setName(node.getName());
            copy.setVisible(node.isVisible());
            copy.addAll(children);

            return copy;
        }

        @NotNull
        private Mesh compactMesh(@NotNull Mesh mesh) {
            final List<Primitive> primitives = new ArrayList<>();

            for (Primitive primitive : mesh.primitives()) {
                final Map<Attribute.Semantic, Accessor> attributes = new LinkedHashMap<>();
                final Accessor indices = compactAccessor(null, primitive.indices());

                primitive.attributes().forEach((semantic, accessor) -> {
                    attributes.put(semantic, compactAccessor(semantic, accessor));
                });

                primitives.add(new Primitive(attributes, indices, primitive.hash()));
            }

            return new Mesh(primitives);
        }

        @NotNull
        private Accessor compactAccessor(@Nullable Attribute.Semantic semantic, @NotNull Accessor accessor) {
            final ComponentType componentType;
            final boolean normalized;

            if (accessor.componentType() == ComponentType.HALF_FLOAT || accessor.componentType() == ComponentType.INT_10_10_10_2) {
                // Unsupported glTF component types, convert to float
                componentType = ComponentType.FLOAT;
                normalized = false;
            } else if ((semantic == Attribute.Semantic.POSITION || semantic == Attribute.Semantic.NORMAL) && accessor.componentType() != ComponentType.FLOAT) {
                // Positions and normals must be floats
                componentType = ComponentType.FLOAT;
                normalized = false;
            } else {
                componentType = accessor.componentType();
                normalized = accessor.normalized();
            }

            final int position = buffer.length();
            final int length = accessor.count() * accessor.componentCount() * componentType.glSize();

            buffer.grow(IOUtils.alignUp(length, 4));

            final BufferView bufferView = buffer.asView(position, length);
            final ByteBuffer buffer = bufferView.asByteBuffer();

            switch (componentType) {
                case BYTE, UNSIGNED_BYTE -> {
                    final Accessor.ByteView view = accessor.asByteView();
                    for (int i = 0; i < accessor.count(); i++) {
                        for (int j = 0; j < accessor.componentCount(); j++) {
                            buffer.put(view.get(i, j));
                        }
                    }
                }
                case SHORT, UNSIGNED_SHORT -> {
                    final Accessor.ShortView view = accessor.asShortView();
                    for (int i = 0; i < accessor.count(); i++) {
                        for (int j = 0; j < accessor.componentCount(); j++) {
                            buffer.putShort(view.get(i, j));
                        }
                    }
                }
                case INT, UNSIGNED_INT -> {
                    final Accessor.IntView view = accessor.asIntView();
                    for (int i = 0; i < accessor.count(); i++) {
                        for (int j = 0; j < accessor.componentCount(); j++) {
                            buffer.putInt(view.get(i, j));
                        }
                    }
                }
                case FLOAT -> {
                    final Accessor.FloatView view = accessor.asFloatView();
                    for (int i = 0; i < accessor.count(); i++) {
                        for (int j = 0; j < accessor.componentCount(); j++) {
                            buffer.putFloat(view.get(i, j));
                        }
                    }
                }
                default -> throw new UnsupportedOperationException("Unsupported component type: " + componentType);
            }

            return new Accessor(
                bufferView,
                accessor.elementType(),
                componentType,
                accessor.target(),
                0,
                accessor.count(),
                normalized
            );
        }
    }

    private static class Context {
        private final List<Scene> scenes = new ArrayList<>();
        private final List<Node> nodes = new ArrayList<>();
        private final List<Mesh> meshes = new ArrayList<>();
        private final List<Accessor> accessors = new ArrayList<>();
        private final List<BufferViewInfo> bufferViews = new ArrayList<>();
        private final List<Buffer> buffers = new ArrayList<>();

        private void addScene(@NotNull Scene scene) {
            add(scene, scenes);
        }

        private int addNode(@NotNull Node node) {
            if (nodes.contains(node)) {
                throw new IllegalStateException("Recursive node hierarchy");
            }

            return add(node, nodes);
        }

        private int addMesh(@NotNull Mesh mesh) {
            return add(mesh, meshes);
        }

        private int addAccessor(@NotNull Accessor accessor) {
            return add(accessor, accessors);
        }

        private int addBufferView(@NotNull BufferViewInfo bufferView) {
            return add(bufferView, bufferViews);
        }

        private int addBuffer(@NotNull Buffer buffer) {
            return add(buffer, buffers);
        }

        private static <T> int add(@NotNull T object, @NotNull List<? super T> list) {
            int index = list.indexOf(object);

            if (index < 0) {
                index = list.size();
                list.add(object);
            }

            return index;
        }
    }

    private record Scene(@NotNull List<Node> nodes) {}

    private record BufferViewInfo(@NotNull BufferView view, @NotNull Accessor accessor) {}
}