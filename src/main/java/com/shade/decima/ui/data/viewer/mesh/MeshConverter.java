package com.shade.decima.ui.data.viewer.mesh;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.ui.data.viewer.mesh.data.*;
import com.shade.decima.ui.data.viewer.mesh.dmf.*;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MeshConverter {
    private static final Map<Class<?>, Map<Class<?>, Converter<?, ?>>> CONVERTERS = Map.of(
        AccessorDataInt8.class, Map.of(
            AccessorDataInt8.class, (Converter<AccessorDataInt8, AccessorDataInt8>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci)),
            AccessorDataInt16.class, (Converter<AccessorDataInt8, AccessorDataInt16>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, (short) (s.get(sei, sci) & 0xFF)),
            AccessorDataFloat32.class, (Converter<AccessorDataInt8, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, (s.get(sei, sci) & 0xFF) / 255.0f)
        ),
        AccessorDataInt16.class, Map.of(
            AccessorDataFloat32.class, (Converter<AccessorDataInt16, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, (s.get(sei, sci) & 0xFFFF) / 32767.0f)
        ),
        AccessorDataFloat16.class, Map.of(
            AccessorDataFloat32.class, (Converter<AccessorDataFloat16, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci))
        ),
        AccessorDataFloat32.class, Map.of(
            AccessorDataFloat32.class, (Converter<AccessorDataFloat32, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci))
        ),
        AccessorDataXYZ10W2.class, Map.of(
            AccessorDataFloat32.class, (Converter<AccessorDataXYZ10W2, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci))
        )
    );

    private static final Map<String, AccessorDescriptor> SEMANTIC_DESCRIPTORS = Map.ofEntries(
        Map.entry("Pos", new AccessorDescriptor("POSITION", ElementType.VEC3, ComponentType.FLOAT, false, false)),
        Map.entry("TangentBFlip", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Tangent", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Normal", new AccessorDescriptor("NORMAL", ElementType.VEC3, ComponentType.FLOAT, false, true)),
        Map.entry("Color", new AccessorDescriptor("COLOR_0", ElementType.VEC4, ComponentType.UNSIGNED_BYTE, true, true)),
        Map.entry("UV0", new AccessorDescriptor("TEXCOORD_0", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV1", new AccessorDescriptor("TEXCOORD_1", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV2", new AccessorDescriptor("TEXCOORD_2", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV3", new AccessorDescriptor("TEXCOORD_3", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV4", new AccessorDescriptor("TEXCOORD_4", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV5", new AccessorDescriptor("TEXCOORD_5", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("UV6", new AccessorDescriptor("TEXCOORD_6", ElementType.VEC2, ComponentType.FLOAT, false, false)),
        Map.entry("BlendIndices", new AccessorDescriptor("JOINTS_0", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendIndices2", new AccessorDescriptor("JOINTS_1", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendIndices3", new AccessorDescriptor("JOINTS_2", ElementType.VEC4, ComponentType.UNSIGNED_SHORT, true, false)),
        Map.entry("BlendWeights", new AccessorDescriptor("WEIGHTS_0", ElementType.VEC4, ComponentType.FLOAT, false, false)),
        Map.entry("BlendWeights2", new AccessorDescriptor("WEIGHTS_1", ElementType.VEC4, ComponentType.FLOAT, false, false)),
        Map.entry("BlendWeights3", new AccessorDescriptor("WEIGHTS_2", ElementType.VEC4, ComponentType.FLOAT, false, false))
    );


    private static final class SubMeshInfo {
        public RTTIObject vertexArray;
        public RTTIObject indexArray;
        public RTTIObject primitive;
        public Map<String, RTTIObject> attributes = new HashMap<>();
        public int vertexDataOffset;
        public int indexDataOffset;

        public SubMeshInfo(@NotNull RTTIObject vertexArray, @NotNull RTTIObject indexArray, @NotNull RTTIObject primitive) {
            this.vertexArray = vertexArray;
            this.indexArray = indexArray;
            this.primitive = primitive;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertexArray, indexArray, vertexDataOffset, indexDataOffset);
        }
    }

    private final SubMeshInfo[] subMeshes;
    private final ByteBuffer dataSource;
    private final DMFSceneFile scene;
    private final ModelExportContext context;
    private final String resourceName;


    public MeshConverter(CoreBinary core, RTTIObject object, Project project, ByteBuffer dataSource, ModelExportContext context, String resourceName) throws IOException {
        this.dataSource = dataSource;
        this.scene = context.scene;
        this.context = context;
        this.resourceName = resourceName;


        PackfileManager manager = project.getPackfileManager();
        RTTITypeRegistry registry = project.getTypeRegistry();

        int dataSourceOffset = 0;
        RTTIReference[] primitivesRefs = object.get("Primitives");
        subMeshes = new SubMeshInfo[primitivesRefs.length];

        for (int i = 0; i < primitivesRefs.length; i++) {
            RTTIReference ref = primitivesRefs[i];
            final var primitiveRes = ref.follow(core, manager, registry);
            RTTIObject primitive = primitiveRes.object();
            RTTIObject vertexArray = primitive.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
            RTTIObject indexArray = primitive.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
            subMeshes[i] = new SubMeshInfo(vertexArray, indexArray, primitive);

            final var vertices = vertexArray.obj("Data");

            final int vertexCount = vertices.i32("VertexCount");
            subMeshes[i].vertexDataOffset = dataSourceOffset;
            for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                final int stride = stream.i32("Stride");
                dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
            }
            subMeshes[i].indexDataOffset = dataSourceOffset;
            final var indices = indexArray.obj("Data");

            final int indexCount = indices.i32("IndexCount");
            int indexSize = switch (indices.str("Format")) {
                case "Index16" -> 2;
                case "Index32" -> 4;
                default -> throw new IllegalStateException("Unexpected value: " + indices.str("Format"));
            };

            dataSourceOffset += IOUtils.alignUp(indexSize * indexCount, 256);

        }


    }

    @SuppressWarnings("unchecked")
    public DMFMesh processMesh() throws IOException {
        DMFMesh mesh = new DMFMesh();

        for (int j = 0; j < subMeshes.length; j++) {
            SubMeshInfo subMesh = subMeshes[j];
            DMFPrimitive meshPrimitive = mesh.newPrimitive();
            Map<String, ByteBuffer> attributeDataMap = new HashMap<>();


            final var vertices = subMesh.vertexArray.obj("Data");
            final var indices = subMesh.indexArray.obj("Data");

            final int vertexCount = vertices.i32("VertexCount");
            final int indexCount = indices.i32("IndexCount");
            final int indexStartIndex = subMesh.primitive.i32("StartIndex");
            final int indexEndIndex = subMesh.primitive.i32("EndIndex");

            meshPrimitive.indexOffset = 0;

            int dataSourceOffset = subMesh.vertexDataOffset;
            if (context.convertVertices) {
                for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                    final int stride = stream.i32("Stride");
                    for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
                        final int offset = element.i8("Offset");
                        String elementType = element.str("Type");
                        subMesh.attributes.put(elementType, element);
                        final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementType);
                        if (descriptor == null) {
                            throw new IllegalArgumentException("Unsupported element type: " + elementType);
                        }


                        if (descriptor.semantic.equals("TANGENT")) continue;
                        final var accessor = switch (element.str("StorageType")) {
                            case "UnsignedByte" ->
                                new AccessorDataInt8(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
                            case "UnsignedByteNormalized" ->
                                new AccessorDataInt8(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                            case "SignedShort" ->
                                new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, false);
                            case "SignedShortNormalized" ->
                                new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
                            case "UnsignedShort" ->
                                new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
                            case "UnsignedShortNormalized" ->
                                new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                            case "HalfFloat" ->
                                new AccessorDataFloat16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset);
                            case "Float" ->
                                new AccessorDataFloat32(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset);
                            case "X10Y10Z10W2Normalized" ->
                                new AccessorDataXYZ10W2(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
                            case "X10Y10Z10W2UNorm" ->
                                new AccessorDataInt32(dataSource, ElementType.SCALAR, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                            default ->
                                throw new IllegalArgumentException("Unsupported component type: " + element.str("StorageType"));
                        };

                        final int size = descriptor.elementType.getStride(descriptor.componentType) * vertexCount;
                        final ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        final AccessorData consumer = switch (descriptor.semantic) {
                            case "POSITION", "NORMAL", "TANGENT",
                                "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6" ->
                                new AccessorDataFloat32(buffer, descriptor.elementType, vertexCount, 0, 0, 0);
                            case "WEIGHTS_0", "WEIGHTS_1", "WEIGHTS_2", "WEIGHTS_3" ->
                                new AccessorDataFloat32(buffer, descriptor.elementType, vertexCount, 0, 0, 0);
                            case "COLOR_0" ->
                                new AccessorDataInt8(buffer, descriptor.elementType, vertexCount, 0, 0, 0, true, true);
                            case "JOINTS_0", "JOINTS_1", "JOINTS_2", "JOINTS_3" ->
                                new AccessorDataInt16(buffer, descriptor.elementType, vertexCount, 0, 0, 0, true, false);
                            default ->
                                throw new IllegalArgumentException("Unsupported semantic: " + descriptor.semantic);
                        };

                        final Converter<AccessorData, AccessorData> converter;

                        if (CONVERTERS.containsKey(accessor.getClass())) {
                            converter = (Converter<AccessorData, AccessorData>) CONVERTERS.get(accessor.getClass()).get(consumer.getClass());
                        } else {
                            converter = null;
                        }
                        if (converter == null) {
                            throw new IllegalArgumentException("Can't find convertor from " + accessor.getClass().getSimpleName() + " to " + consumer.getClass().getSimpleName());
                        }
                        for (int elem = 0; elem < accessor.getElementCount(); elem++) {
                            for (int comp = 0; comp < accessor.getComponentCount(); comp++) {
                                converter.convert(accessor, elem, comp, consumer, elem, comp);
                            }
                        }

                        attributeDataMap.put(elementType, buffer.position(0));
                    }

                    dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                }

                dataSourceOffset = subMesh.indexDataOffset;

                ComponentType componentType = switch (indices.str("Format")) {
                    case "Index16" -> ComponentType.UNSIGNED_SHORT;
                    case "Index32" -> ComponentType.UNSIGNED_INT;
                    default -> throw new IllegalArgumentException("Unsupported index format: " + indices.str("Format"));
                };

                final var accessor = switch (indices.str("Format")) {
                    case "Index16" ->
                        new AccessorDataInt16(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
                    case "Index32" ->
                        new AccessorDataInt32(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
                    default -> throw new IllegalArgumentException("Unsupported index format: " + indices.str("Format"));
                };

                final var buffer = ByteBuffer
                    .allocate(accessor.getElementCount() * accessor.getComponentType().getSize())
                    .order(ByteOrder.LITTLE_ENDIAN);

                for (int i = 0; i < indexCount; i++) {
                    if (accessor instanceof AccessorDataInt16 i16) {
                        buffer.putShort(i16.get(i, 0));
                    } else {
                        buffer.putInt(((AccessorDataInt32) accessor).get(i, 0));
                    }
                }
                DMFBuffer indexBuffer;
                if (context.embedBuffers) {
                    indexBuffer = new DMFInternalBuffer(buffer.position(0));
                } else {
                    ByteBuffer indicesBufferData = buffer.position(0);
                    String bufferFileName = resourceName + "_PRIMITIVE%d_INDICES.dbuf".formatted(j);
                    indexBuffer = new DMFExternalBuffer(bufferFileName, indicesBufferData.remaining());
                    Files.write(context.outputDir.resolve(bufferFileName), indicesBufferData.array());
                }
                meshPrimitive.indexSize = componentType.getSize();
                DMFBufferView bufferView = new DMFBufferView();
                bufferView.setBuffer(indexBuffer, scene);
                bufferView.size = indexCount * componentType.getSize();
                meshPrimitive.setIndexBufferView(bufferView, scene);
                meshPrimitive.indexStart = indexStartIndex;
                meshPrimitive.indexEnd = indexEndIndex;
                meshPrimitive.indexCount = indexEndIndex - indexStartIndex;

                meshPrimitive.vertexCount = vertexCount;
                meshPrimitive.vertexStart = 0;
                meshPrimitive.vertexEnd = vertexCount;


                if (!attributeDataMap.containsKey("Pos")) {
                    throw new RuntimeException("Mesh does not contain positional data");
                } else {
                    copyBufferAsIs("Pos", meshPrimitive, attributeDataMap.get("Pos").position(0), j);
                }
                if (attributeDataMap.containsKey("Normal")) {
                    RTTIObject elementInfo = subMesh.attributes.get("Normal");
                    if (elementInfo.str("StorageType").equals("Float")) {
                        copyBufferAsIs("Normal", meshPrimitive, attributeDataMap.get("Normal").position(0), j);
                    }
                }

                if (attributeDataMap.containsKey("Color")) {
                    copyBufferAsIs("Color", meshPrimitive, attributeDataMap.get("Color").position(0), j);
                }
                if (attributeDataMap.containsKey("UV0")) {
                    copyBufferAsIs("UV0", meshPrimitive, attributeDataMap.get("UV0").position(0), j);
                }
                if (attributeDataMap.containsKey("UV1")) {
                    copyBufferAsIs("UV1", meshPrimitive, attributeDataMap.get("UV1").position(0), j);
                }
                if (attributeDataMap.containsKey("UV2")) {
                    copyBufferAsIs("UV2", meshPrimitive, attributeDataMap.get("UV2").position(0), j);
                }
                if (attributeDataMap.containsKey("UV3")) {
                    copyBufferAsIs("UV3", meshPrimitive, attributeDataMap.get("UV3").position(0), j);
                }
                if (attributeDataMap.containsKey("UV4")) {
                    copyBufferAsIs("UV4", meshPrimitive, attributeDataMap.get("UV4").position(0), j);
                }
                if (attributeDataMap.containsKey("UV5")) {
                    copyBufferAsIs("UV5", meshPrimitive, attributeDataMap.get("UV5").position(0), j);
                }
                if (attributeDataMap.containsKey("UV6")) {
                    copyBufferAsIs("UV6", meshPrimitive, attributeDataMap.get("UV6").position(0), j);
                }

                for (int bufId = 0; bufId < 3; bufId++) {
                    String blendIndicesName = bufId == 0 ? "BlendIndices" : "BlendIndices%d".formatted(bufId + 1);
                    String blendWeightsName = bufId == 0 ? "BlendWeights" : "BlendWeights%d".formatted(bufId + 1);
                    if (attributeDataMap.containsKey(blendIndicesName)) {
                        copyBufferAsIs(blendIndicesName, meshPrimitive, attributeDataMap.get(blendIndicesName).position(0), j);
                        if (attributeDataMap.containsKey(blendWeightsName)) {
                            copyBufferAsIs(blendWeightsName, meshPrimitive, attributeDataMap.get(blendWeightsName).position(0), j);
                        } else {
                            RTTIObject attributeInfo = subMesh.attributes.get(blendIndicesName);
                            ByteBuffer weightBuffer = ByteBuffer.allocate(4 * Float.BYTES * vertexCount).order(ByteOrder.LITTLE_ENDIAN);
                            byte usedSlots = attributeInfo.i8("UsedSlots");
                            for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
                                for (int i = 0; i < 4; i++) {
                                    if (i < usedSlots) {
                                        weightBuffer.putFloat(1);
                                    } else {
                                        weightBuffer.putFloat(0);
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return mesh;
    }

    private void copyBufferAsIs(String attributeName, DMFPrimitive primitive, ByteBuffer data, int primitiveId) throws IOException {
        final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(attributeName);
        final RTTIObject attributeInfo = subMeshes[primitiveId].attributes.get(attributeName);
        final DMFBuffer elementBuffer;
        if (context.embedBuffers) {
            elementBuffer = new DMFInternalBuffer(data);
        } else {
            String bufferFileName = "%s_PRIMITIVE%d_%s.dbuf".formatted(resourceName, primitiveId, descriptor.semantic);
            elementBuffer = new DMFExternalBuffer(bufferFileName, data.remaining());
            Files.write(context.outputDir.resolve(bufferFileName), data.array());
        }
        final DMFVertexAttribute vertexAttribute = new DMFVertexAttribute();
        vertexAttribute.semantic = descriptor.semantic;
        vertexAttribute.elementCount = descriptor.elementType.getComponentCount();
        vertexAttribute.elementType = attributeInfo.str("StorageType");
        vertexAttribute.size = descriptor.elementType.getStride(descriptor.componentType);
        DMFBufferView bufferView = new DMFBufferView();
        bufferView.setBuffer(elementBuffer, scene);
        bufferView.size = elementBuffer.bufferSize;
        vertexAttribute.setBufferView(bufferView, scene);
        primitive.vertexAttributes.put(descriptor.semantic, vertexAttribute);
    }


    private record AccessorDescriptor(@NotNull String semantic, @NotNull ElementType elementType, @NotNull ComponentType componentType, boolean unsigned, boolean normalized) {}

    private interface Converter<SRC_T extends AccessorData, DST_T extends AccessorData> {
        void convert(@NotNull SRC_T src, int strElementIndex, int srcComponentIndex, @NotNull DST_T dst, int dstElementIndex, int dstComponentIndex);
    }


}