package com.shade.decima.ui.data.viewer.model;

import com.shade.decima.BuildConfig;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.viewer.model.data.Accessor;
import com.shade.decima.ui.data.viewer.model.data.ComponentType;
import com.shade.decima.ui.data.viewer.model.data.ElementType;
import com.shade.decima.ui.data.viewer.model.data.impl.*;
import com.shade.decima.ui.data.viewer.model.gltf.*;
import com.shade.decima.ui.data.viewer.model.utils.Transform;
import com.shade.platform.model.runtime.ProgressMonitor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.shade.decima.model.rtti.messages.impl.IndexArrayResourceHandler.HwIndexArray;
import static com.shade.decima.model.rtti.messages.impl.VertexArrayResourceHandler.HwVertexArray;

public class GLTFExporter extends ModelExporterShared implements ModelExporter {
    private static final Logger log = LoggerFactory.getLogger(GLTFExporter.class);
    private static final Map<String, String> dsToGltf = Map.ofEntries(
        Map.entry("Pos", "POSITION"),
        Map.entry("Normal", "NORMAL"),
        Map.entry("Tangent", "TANGENT"),
        Map.entry("TangentBFlip", "TANGENT"),
        Map.entry("Color", "COLOR_0"),
        Map.entry("UV0", "TEXCOORD_0"),
        Map.entry("UV1", "TEXCOORD_1"),
        Map.entry("UV2", "TEXCOORD_2"),
        Map.entry("UV3", "TEXCOORD_3"),
        Map.entry("UV4", "TEXCOORD_4"),
        Map.entry("UV5", "TEXCOORD_5"),
        Map.entry("UV6", "TEXCOORD_6"),
        Map.entry("BlendIndices", "JOINTS_0"),
        Map.entry("BlendIndices2", "JOINTS_1"),
        Map.entry("BlendIndices3", "JOINTS_2"),
        Map.entry("BlendWeights", "WEIGHTS_0"),
        Map.entry("BlendWeights2", "WEIGHTS_1"),
        Map.entry("BlendWeights3", "WEIGHTS_2")
    );
    private static final Map<ComponentType, Integer> componentTypeToGltf = Map.of(
        ComponentType.INT8, 5120,
        ComponentType.UINT8, 5121,
        ComponentType.INT16, 5122,
        ComponentType.UINT16, 5123,
        ComponentType.UINT32, 5125,
        ComponentType.FLOAT32, 5126
    );

    public static class Provider implements ModelExporterProvider {
        @NotNull
        @Override
        public ModelExporter create(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
            return new GLTFExporter(project, exportSettings, outputPath);
        }

        @NotNull
        @Override
        public String getExtension() {
            return "gltf";
        }

        @NotNull
        @Override
        public String getName() {
            return "GLTF scene";
        }
    }

    private final RTTITypeRegistry registry;
    private final PackfileManager manager;
    private final ExportSettings exportSettings;
    private final Path outputPath;

    private GltfFile file;

    public GLTFExporter(@NotNull Project project, @NotNull ExportSettings exportSettings, @NotNull Path outputPath) {
        registry = project.getTypeRegistry();
        manager = project.getPackfileManager();
        this.exportSettings = exportSettings;
        this.outputPath = outputPath;

    }

    @Override
    public Object export(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        file = new GltfFile();
        file.asset = new GltfAsset(BuildConfig.APP_TITLE + "@" + BuildConfig.BUILD_COMMIT, "2.0");
        file.setScene(file.newScene());
        exportResource(monitor, core, object, resourceName);
        return file;
    }

    private void exportResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        log.info("Exporting {}", object.type().getTypeName());
        switch (object.type().getTypeName()) {
//            case "ArtPartsDataResource" -> exportArtPartsDataResource(monitor, core, object, resourceName);
//            case "ArtPartsSubModelResource" -> exportArtPartsSubModelResource(monitor, core, object, resourceName);
//            case "ObjectCollection" -> exportObjectCollection(monitor, core, object, resourceName);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(monitor, core, object);
//            case "Terrain" -> exportTerrainResource(monitor, core, object);
//            case "LodMeshResource" -> exportLodMeshResource(monitor, core, object, resourceName);
//            case "MultiMeshResource" -> exportMultiMeshResource(monitor, core, object, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(monitor, core, object, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.type());
        }
    }

    private void exportRegularSkinnedMeshResource(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {
        GltfNode sceneRoot = file.newNode("SceneRoot");
        sceneRoot.setTransform(Transform.fromRotation(0, -90, 0));
        file.addToScene(sceneRoot);
        GltfNode model;
        try (ProgressMonitor.Task task = monitor.begin("Exporting RegularSkinnedMeshResource mesh", 1)) {
            model = regularSkinnedMeshResourceToModel(task.split(1), core, object, resourceName);
        }
        if (model != null) {
            file.addChild(sceneRoot, model);
        }
    }

    private GltfNode regularSkinnedMeshResourceToModel(
        @NotNull ProgressMonitor monitor,
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull String resourceName
    ) throws Throwable {


        final String dataSourceLocation = "%s.core.stream".formatted(object.obj("DataSource").str("Location"));
        final Packfile dataSourcePackfile = Objects.requireNonNull(manager.findAny(dataSourceLocation), "Can't find referenced data source");
        final ByteBuffer dataSource = ByteBuffer
            .wrap(dataSourcePackfile.extract(dataSourceLocation))
            .order(ByteOrder.LITTLE_ENDIAN);

        final GltfMesh gltfMesh = new GltfMesh(file);

        int dataSourceOffset = 0;
        Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();
        final RTTIReference[] primitivesRefs = object.get("Primitives");

        try (ProgressMonitor.Task task = monitor.begin("Exporting RegularSkinnedMeshResource primitives", 2)) {
            try (ProgressMonitor.Task collectTask = task.split(1).begin("Collecting primitives", primitivesRefs.length)) {
                for (RTTIReference primitivesRef : primitivesRefs) {
                    final var primitiveRes = primitivesRef.follow(core, manager, registry);
                    final RTTIObject primitiveObj = primitiveRes.object();
                    RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                    RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                    final var vertices = vertexArray.obj("Data");
                    final var indices = indexArray.obj("Data");

                    final int vertexCount = vertices.i32("VertexCount");
                    final int indexCount = indices.i32("IndexCount");

                    RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                    if (!bufferOffsets.containsKey(vertexArrayUUID)) {
                        bufferOffsets.put(vertexArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                        for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                            final int stride = stream.i32("Stride");
                            dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                        }
                    }
                    RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");
                    if (!bufferOffsets.containsKey(indicesArrayUUID)) {
                        bufferOffsets.put(indicesArrayUUID, Map.entry(dataSourceOffset, bufferOffsets.size()));
                        int indexSize = switch (indices.str("Format")) {
                            case "Index16" -> 2;
                            case "Index32" -> 4;
                            default -> throw new IllegalStateException("Unexpected value: " + indices.str("Format"));
                        };

                        dataSourceOffset += IOUtils.alignUp(indexSize * indexCount, 256);
                    }
                    collectTask.worked(1);

                }
            }
            for (RTTIReference ref : primitivesRefs) {
                final var primitiveRes = ref.follow(core, manager, registry);
                final RTTIObject primitiveObj = primitiveRes.object();
                final RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                final RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                final HwVertexArray vertices = vertexArray.obj("Data").cast();
                final HwIndexArray indices = indexArray.obj("Data").cast();

                RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");

                Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayUUID);
                dataSourceOffset = offsetAndGroupId.getKey();

                final int vertexCount = vertices.vertexCount;
                final int indexCount = indices.indexCount;
                final int indexStartIndex = primitiveObj.i32("StartIndex");
                final int indexEndIndex = primitiveObj.i32("EndIndex");

                final Map<String, Accessor> attributes = new HashMap<>();
                int uvLayerCount = 0;
                for (RTTIObject stream : vertices.streams) {
                    final int stride = stream.i32("Stride");

                    for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
                        final int offset = element.i8("Offset");

                        final var elementType = switch (element.str("Type")) {
                            case "Pos", "Normal" -> ElementType.VEC3;
                            case "UV0", "UV1", "UV2", "UV3", "UV4", "UV5", "UV6" -> ElementType.VEC2;
                            case "BlendIndices", "BlendIndices2", "BlendIndices3",
                                "BlendWeights", "BlendWeights2", "BlendWeights3", "Color", "Tangent", "TangentBFlip" ->
                                ElementType.VEC4;
                            default ->
                                throw new IllegalArgumentException("Unsupported element type: " + element.str("Type"));
                        };

                        final var semanticName = dsToGltf.get(element.str("Type"));

                        final var accessor = switch (element.str("StorageType")) {
                            case "UnsignedByte", "UnsignedByteNormalized" ->
                                new ByteAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, true);
                            case "SignedShort", "SignedShortNormalized" ->
                                new ShortAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, false);
                            case "UnsignedShort", "UnsignedShortNormalized" ->
                                new ShortAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride, true);
                            case "HalfFloat" ->
                                new HalfFloatAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "Float" ->
                                new FloatAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "X10Y10Z10W2Normalized" ->
                                new X10Y10Z10W2NormalizedAccessor(dataSource, elementType, vertexCount, dataSourceOffset + offset, stride);
                            case "X10Y10Z10W2UNorm" ->
                                new X10Y10Z10W2UNormAccessor(dataSource, ElementType.SCALAR, vertexCount, dataSourceOffset + offset, stride);
                            default ->
                                throw new IllegalArgumentException("Unsupported component type: " + element.str("StorageType"));
                        };

                        attributes.put(semanticName, accessor);
                    }

                    dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                }


                final GltfMesh.Primitive gltfMeshPrimitive = new GltfMesh.Primitive(gltfMesh);

                for (Map.Entry<String, Accessor> entry : attributes.entrySet()) {
                    String semantic = entry.getKey();
                    Accessor supplier = entry.getValue();
                    final ByteBuffer buffer;
                    final Accessor consumer;
                    final ComponentType componentType;
                    final ElementType elementType;
                    boolean normalized = false;
                    if (semantic.equals("TANGENT")) {
                        continue;
                    }

                    switch (semantic) {
                        case "POSITION", "NORMAL" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC3;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 12);
                        }
                        case "WEIGHTS_0", "WEIGHTS_1", "WEIGHTS_2", "WEIGHTS_3" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 16);
                        }
                        case "COLOR_0" -> {
                            normalized = true;
                            componentType = ComponentType.UINT8;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new ByteAccessor(buffer, elementType, vertexCount, 0, 4, true);
                        }
                        case "JOINTS_0", "JOINTS_1", "JOINTS_2", "JOINTS_3" -> {
                            componentType = ComponentType.UINT16;
                            elementType = ElementType.VEC4;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new ShortAccessor(buffer, elementType, vertexCount, 0, 8, true);
                        }
                        case "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6" -> {
                            componentType = ComponentType.FLOAT32;
                            elementType = ElementType.VEC2;
                            final int size = elementType.getStride(componentType) * vertexCount;
                            buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                            consumer = new FloatAccessor(buffer, elementType, vertexCount, 0, 8);
                        }
                        default -> throw new IllegalArgumentException("Unsupported semantic: " + semantic);
                    }
                    if (semantic.contains("WEIGHTS_")) {
                        for (int i = 0; i < vertexCount; i++) {

                        }
                    } else {
                        AbstractAccessor.transfer(supplier, consumer);
                    }

                    final GltfBuffer gltfBuffer = new GltfBuffer(file);
                    gltfBuffer.uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(buffer.position(0), buffer.capacity()));
                    gltfBuffer.byteLength = buffer.capacity();

                    final GltfBufferView gltfBufferView = new GltfBufferView(file, gltfBuffer);
                    gltfBufferView.byteOffset = 0;
                    gltfBufferView.byteLength = buffer.capacity();

                    final GltfAccessor gltfAccessor = new GltfAccessor(file, gltfBufferView);
                    gltfAccessor.type = elementType.name();
                    gltfAccessor.componentType = componentTypeToGltf.get(componentType);
                    gltfAccessor.count = vertexCount;
                    gltfAccessor.normalized = normalized;
                    if (semantic.equals("POSITION")) {
                        float mnX = 2e+31f, mnY = 2e+31f, mnZ = 2e+31f,
                            mxX = -2e+31f, mxY = -2e+31f, mxZ = -2e+31f;
                        FloatAccessor positionAccessor = (FloatAccessor) consumer;
                        for (int i = 0; i < vertexCount; i++) {
                            float x = positionAccessor.get(i, 0);
                            float y = positionAccessor.get(i, 1);
                            float z = positionAccessor.get(i, 2);
                            mnX = Math.min(mnX, x);
                            mnY = Math.min(mnY, y);
                            mnZ = Math.min(mnZ, z);

                            mxX = Math.max(mxX, x);
                            mxY = Math.max(mxY, y);
                            mxZ = Math.max(mxZ, z);
                        }
                        gltfAccessor.min = new double[]{mnX, mnY, mnZ};
                        gltfAccessor.max = new double[]{mxX, mxY, mxZ};
                    }
                    if (semantic.contains("TEXCOORD_")) {
                        semantic = "TEXCOORD_%d".formatted(uvLayerCount);
                        uvLayerCount++;
                    }
                    gltfMeshPrimitive.attributes.put(semantic, file.accessors.indexOf(gltfAccessor));
                }

                offsetAndGroupId = bufferOffsets.get(indicesArrayUUID);
                final var indicesBuffer = dataSource.slice(offsetAndGroupId.getKey(), indexCount * indices.getSize());


                final GltfBuffer indicesGltfBuffer = new GltfBuffer(file);
                indicesGltfBuffer.uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(indicesBuffer.position(0), indicesBuffer.capacity()));
                indicesGltfBuffer.byteLength = indicesBuffer.capacity();

                final GltfBufferView indicesBufferView = new GltfBufferView(file, indicesGltfBuffer);
                indicesBufferView.byteOffset = 0;
                indicesBufferView.byteLength = indicesBuffer.capacity();

                final GltfAccessor indicesGltfAccessor = new GltfAccessor(file, indicesBufferView);
                indicesGltfAccessor.type = ElementType.SCALAR.name();
                indicesGltfAccessor.componentType = componentTypeToGltf.get(indices.getSize() == 2 ? ComponentType.UINT16 : ComponentType.UINT32);
                indicesGltfAccessor.count = indexEndIndex - indexStartIndex;

                gltfMeshPrimitive.indices = file.accessors.indexOf(indicesGltfAccessor);

            }
        }
        return file.newNode(resourceName, gltfMesh);
    }

}
