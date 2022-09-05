package com.shade.decima.ui.data.viewer.mesh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.data.viewer.mesh.data.*;
import com.shade.decima.ui.data.viewer.mesh.gltf.*;
import com.shade.decima.ui.editor.property.PropertyEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.platform.ui.util.UIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MeshViewerPanel extends JComponent {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Class<?>, Map<Class<?>, Converter<?, ?>>> CONVERTERS = Map.of(
        AccessorDataInt8.class, Map.of(
            AccessorDataInt8.class, (Converter<AccessorDataInt8, AccessorDataInt8>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci))
        ),
        AccessorDataInt16.class, Map.of(
            AccessorDataFloat32.class, (Converter<AccessorDataInt16, AccessorDataFloat32>) (s, sei, sci, d, dei, dci) -> d.put(dei, dci, s.get(sei, sci) / 32767.0f)
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

    private final JButton exportButton;
    private PropertyEditor editor;

    public MeshViewerPanel() {
        final JLabel placeholder = UIUtils.Labels.h1("Preview is not supported");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);

        exportButton = new JButton("Export\u2026");
        exportButton.setEnabled(false);
        exportButton.addActionListener(event -> {
            final JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(editor.getInput().getName() + ".gltf"));
            chooser.setDialogTitle("Choose output file");
            chooser.setFileFilter(new FileExtensionFilter("glTF File", "gltf"));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showSaveDialog(Application.getFrame()) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try {
                export(chooser.getSelectedFile().toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            JOptionPane.showMessageDialog(Application.getFrame(), "Done");
        });

        setLayout(new MigLayout("ins panel", "[grow,fill]", "[grow,fill][]"));
        add(placeholder, "wrap");
        add(exportButton);
    }

    public void setInput(@Nullable PropertyEditor editor) {
        this.editor = editor;
        this.exportButton.setEnabled(editor != null);
    }

    private void export(@NotNull Path output) throws IOException {
        final var object = (RTTIObject) Objects.requireNonNull(editor.getSelectedValue());

        final GltfFile file = new GltfFile();

        new GltfScene(file);

        final GltfAsset asset = new GltfAsset(file);
        asset.generator = "Decima Explorer";
        asset.version = "2.0";

        exportResource(editor.getCoreBinary(), object, editor.getInput().getProject(), file);

        Files.writeString(output, GSON.toJson(file));
    }

    private void exportResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull GltfFile file
    ) throws IOException {
        switch (object.getType().getTypeName()) {
            case "LodMeshResource" -> exportLodMeshResource(core, object, project, file);
            case "MultiMeshResource" -> exportMultiMeshResource(core, object, project, file);
            case "RegularSkinnedMeshResource", "StaticMeshResource" -> exportRegularSkinnedMeshResource(core, object, project, file);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.getType());
        }
    }

    private void exportLodMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull GltfFile file
    ) throws IOException {
        for (RTTIObject part : object.<RTTIObject[]>get("Meshes")) {
            final var mesh = part.ref("Mesh").follow(core, project.getPackfileManager(), project.getTypeRegistry());
            exportResource(mesh.binary(), mesh.object(), project, file);
            break;
        }
    }

    private void exportMultiMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull GltfFile file
    ) throws IOException {
        for (RTTIObject part : object.<RTTIObject[]>get("Parts")) {
            final var mesh = part.ref("Mesh").follow(core, project.getPackfileManager(), project.getTypeRegistry());
            final var transform = part.obj("Transform");
            final var pos = transform.obj("Position");
            final var ori = transform.obj("Orientation");

            exportResource(mesh.binary(), mesh.object(), project, file);

            IOUtils.last(file.nodes).matrix = new double[]{
                ori.obj("Col0").f32("X"), ori.obj("Col0").f32("Y"), ori.obj("Col0").f32("Z"), pos.f64("X"),
                ori.obj("Col1").f32("X"), ori.obj("Col1").f32("Y"), ori.obj("Col1").f32("Z"), pos.f64("Y"),
                ori.obj("Col2").f32("X"), ori.obj("Col2").f32("Y"), ori.obj("Col2").f32("Z"), pos.f64("Z"),
                0, 0, 0, 1
            };
        }
    }

    @SuppressWarnings("unchecked")
    private void exportRegularSkinnedMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull GltfFile file
    ) throws IOException {
        final var registry = project.getTypeRegistry();
        final var manager = project.getPackfileManager();

        final String dataSourceLocation = "%s.core.stream".formatted(object.obj("DataSource").str("Location"));
        final Packfile dataSourcePackfile = Objects.requireNonNull(manager.findAny(dataSourceLocation), "Can't find referenced data source");
        final ByteBuffer dataSource = ByteBuffer
            .wrap(dataSourcePackfile.extract(dataSourceLocation))
            .order(ByteOrder.LITTLE_ENDIAN);

        final GltfMesh gltfMesh = new GltfMesh(file);

        int dataSourceOffset = 0;

        for (RTTIReference ref : object.<RTTIReference[]>get("Primitives")) {
            final var primitive = ref.follow(core, manager, registry);
            final var vertices = primitive.object().ref("VertexArray").follow(primitive.binary(), manager, registry).object().obj("Data");
            final var indices = primitive.object().ref("IndexArray").follow(primitive.binary(), manager, registry).object().obj("Data");

            final int vertexCount = vertices.i32("VertexCount");
            final int indexCount = indices.i32("IndexCount");
            final int indexStartIndex = primitive.object().i32("StartIndex");
            final int indexEndIndex = primitive.object().i32("EndIndex");

            final Map<String, AccessorData> attributes = new LinkedHashMap<>();

            for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                final int stride = stream.i32("Stride");

                for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
                    final int offset = element.i8("Offset");
                    final int slots = element.i8("UsedSlots");

                    final var elementType = switch (element.str("Type")) {
                        case "Pos", "Normal", "Tangent", "TangentBFlip", "Color" -> ElementType.VEC3;
                        case "UV0", "UV1", "UV2", "UV3", "UV4", "UV5", "UV6" -> ElementType.VEC2;
                        case "BlendIndices", "BlendIndices2", "BlendIndices3" -> ElementType.VEC4;
                        case "BlendWeights", "BlendWeights2", "BlendWeights3" -> ElementType.VEC4;
                        default -> throw new IllegalArgumentException("Unsupported element type: " + element.str("Type"));
                    };

                    final var semanticName = switch (element.str("Type")) {
                        case "Pos" -> "POSITION";
                        case "Normal" -> "NORMAL";
                        case "Tangent", "TangentBFlip" -> "TANGENT";
                        case "Color" -> "COLOR_0";
                        case "UV0" -> "TEXCOORD_0";
                        case "UV1" -> "TEXCOORD_1";
                        case "UV2" -> "TEXCOORD_2";
                        case "UV3" -> "TEXCOORD_3";
                        case "UV4" -> "TEXCOORD_4";
                        case "UV5" -> "TEXCOORD_5";
                        case "UV6" -> "TEXCOORD_6";
                        case "BlendIndices" -> "JOINTS_0";
                        case "BlendIndices2" -> "JOINTS_1";
                        case "BlendIndices3" -> "JOINTS_2";
                        case "BlendWeights" -> "WEIGHTS_0";
                        case "BlendWeights2" -> "WEIGHTS_1";
                        case "BlendWeights3" -> "WEIGHTS_2";
                        default -> throw new IllegalArgumentException("Unsupported element type: " + element.str("Type"));
                    };

                    final var accessor = switch (element.str("StorageType")) {
                        case "UnsignedByte" -> new AccessorDataInt8(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
                        case "UnsignedByteNormalized" -> new AccessorDataInt8(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                        case "SignedShort" -> new AccessorDataInt16(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, false);
                        case "SignedShortNormalized" -> new AccessorDataInt16(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
                        case "UnsignedShort" -> new AccessorDataInt16(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
                        case "UnsignedShortNormalized" -> new AccessorDataInt16(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                        case "HalfFloat" -> new AccessorDataFloat16(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset);
                        case "Float" -> new AccessorDataFloat32(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset);
                        case "X10Y10Z10W2Normalized" -> new AccessorDataXYZ10W2(dataSource, elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
                        case "X10Y10Z10W2UNorm" -> new AccessorDataInt32(dataSource, ElementType.SCALAR, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
                        default -> throw new IllegalArgumentException("Unsupported component type: " + element.str("StorageType"));
                    };

                    attributes.put(semanticName, accessor);
                }

                dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
            }

            // TODO STEPS:
            //  1. Pick suitable accessors for writing data
            //  2. Allocate an output buffer big enough for holding repacked data
            //  3. Write data from accessors into the output buffer
            //  4. Write index data into the output buffer
            //  5. Create required glTF types respectively

            final GltfMesh.Primitive gltfMeshPrimitive = new GltfMesh.Primitive(gltfMesh);

            attributes.forEach((semantic, supplier) -> {
                final ByteBuffer buffer;
                final AccessorData consumer;

                switch (semantic) {
                    case "POSITION", "NORMAL" -> {
                        final int size = ElementType.VEC3.getStride(ComponentType.FLOAT) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataFloat32(buffer, ElementType.VEC3, vertexCount, 0, 0, 0);
                    }
                    case "TANGENT" -> {
                        final int size = ElementType.VEC4.getStride(ComponentType.FLOAT) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataFloat32(buffer, ElementType.VEC4, vertexCount, 0, 0, 0);
                    }
                    case "COLOR_0" -> {
                        final int size = ElementType.VEC4.getStride(ComponentType.BYTE) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataInt8(buffer, ElementType.VEC4, vertexCount, 0, 0, 0, true, true);
                    }
                    case "JOINTS_0", "JOINTS_1", "JOINTS_2", "JOINTS_3" -> {
                        final int size = ElementType.VEC4.getStride(ComponentType.BYTE) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataInt8(buffer, ElementType.VEC4, vertexCount, 0, 0, 0, true, false);
                    }
                    case "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6" -> {
                        final int size = ElementType.VEC2.getStride(ComponentType.FLOAT) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataFloat32(buffer, ElementType.VEC2, vertexCount, 0, 0, 0);
                    }
                    case "WEIGHTS_0", "WEIGHTS_1", "WEIGHTS_2", "WEIGHTS_3" -> {
                        final int size = ElementType.VEC4.getStride(ComponentType.BYTE) * vertexCount;
                        buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
                        consumer = new AccessorDataInt8(buffer, ElementType.VEC4, vertexCount, 0, 0, 0, true, true);
                    }
                    default -> throw new IllegalArgumentException("Unsupported semantic: " + semantic);
                }

                final Converter<AccessorData, AccessorData> converter;

                if (CONVERTERS.containsKey(supplier.getClass())) {
                    converter = (Converter<AccessorData, AccessorData>) CONVERTERS.get(supplier.getClass()).get(consumer.getClass());
                } else {
                    converter = null;
                }

                if (converter == null) {
                    throw new IllegalArgumentException("Can't find convertor from " + supplier.getClass().getSimpleName() + " to " + consumer.getClass().getSimpleName());
                }

                for (int elem = 0; elem < supplier.getElementCount(); elem++) {
                    for (int comp = 0; comp < supplier.getComponentCount(); comp++) {
                        converter.convert(supplier, elem, comp, consumer, elem, comp);
                    }
                }

                final GltfBuffer gltfBuffer = new GltfBuffer(file);
                gltfBuffer.uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(buffer.position(0), buffer.capacity()));
                gltfBuffer.byteLength = buffer.capacity();

                final GltfBufferView gltfBufferView = new GltfBufferView(file, gltfBuffer);
                gltfBufferView.byteOffset = 0;
                gltfBufferView.byteLength = buffer.capacity();
                gltfBufferView.byteStride = 0;

                final GltfAccessor gltfAccessor = new GltfAccessor(file, gltfBufferView);
                gltfAccessor.type = consumer.getElementType().name();
                gltfAccessor.componentType = consumer.getComponentType().getId();
                gltfAccessor.count = vertexCount;

                gltfMeshPrimitive.attributes.put(semantic, file.accessors.indexOf(gltfAccessor));
            });

            final var accessor = switch (indices.str("Format")) {
                case "Index16" -> new AccessorDataInt16(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
                case "Index32" -> new AccessorDataInt32(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
                default -> throw new IllegalArgumentException("Unsupported index format: " + indices.str("Format"));
            };

            final var buffer = ByteBuffer
                .allocate(accessor.getElementCount() * accessor.getComponentType().getSize())
                .order(ByteOrder.LITTLE_ENDIAN);

            for (int i = indexStartIndex; i < indexEndIndex; i++) {
                if (accessor instanceof AccessorDataInt16 i16) {
                    buffer.putShort(i16.get(i, 0));
                } else {
                    buffer.putInt(((AccessorDataInt32) accessor).get(i, 0));
                }
            }

            final GltfBuffer gltfBuffer = new GltfBuffer(file);
            gltfBuffer.uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(IOUtils.getBytesExact(buffer.position(0), buffer.capacity()));
            gltfBuffer.byteLength = buffer.capacity();

            final GltfBufferView gltfBufferView = new GltfBufferView(file, gltfBuffer);
            gltfBufferView.byteOffset = 0;
            gltfBufferView.byteLength = buffer.capacity();
            gltfBufferView.byteStride = 0;

            final GltfAccessor gltfAccessor = new GltfAccessor(file, gltfBufferView);
            gltfAccessor.type = accessor.getElementType().name();
            gltfAccessor.componentType = accessor.getComponentType().getId();
            gltfAccessor.count = indexEndIndex - indexStartIndex;

            gltfMeshPrimitive.indices = file.accessors.indexOf(gltfAccessor);

            dataSourceOffset += IOUtils.alignUp(accessor.getComponentType().getSize() * accessor.getElementCount(), 256);
        }

        new GltfNode(file, IOUtils.last(file.scenes), gltfMesh);
    }

    private interface Converter<SRC_T extends AccessorData, DST_T extends AccessorData> {
        void convert(@NotNull SRC_T src, int strElementIndex, int srcComponentIndex, @NotNull DST_T dst, int dstElementIndex, int dstComponentIndex);
    }
}
