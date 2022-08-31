package com.shade.decima.ui.data.viewer.mesh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

public class MeshViewerPanel extends JComponent {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ElementTypeDescriptor> ELEMENT_TYPES = Map.of(
        "Pos", new ElementTypeDescriptor("POSITION", 3, Float.BYTES * 3, 5126, "VEC3"),
        "Normal", new ElementTypeDescriptor("NORMAL", 3, Float.BYTES * 3, 5126, "VEC3")
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

        final GltfAsset asset = new GltfAsset();
        asset.generator = "Decima Explorer";
        asset.version = "2.0";

        final GltfFile file = new GltfFile();
        file.asset = asset;
        file.scene = 0;
        file.scenes.add(new GltfScene());

        exportResource(editor.getCoreBinary(), object, editor, file);

        Files.writeString(output, GSON.toJson(file));
    }

    private void exportResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull PropertyEditor editor,
        @NotNull GltfFile file
    ) throws IOException {
        switch (object.getType().getTypeName()) {
            case "LodMeshResource" -> exportLodMeshResource(core, object, editor, file);
            case "MultiMeshResource" -> exportMultiMeshResource(core, object, editor, file);
            case "RegularSkinnedMeshResource", "StaticMeshResource" -> exportRegularSkinnedMeshResource(core, object, editor, file);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.getType());
        }
    }

    private void exportLodMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull PropertyEditor editor,
        @NotNull GltfFile file
    ) throws IOException {
        final var registry = editor.getInput().getProject().getTypeRegistry();
        final var packfile = editor.getInput().getNode().getPackfile();

        for (RTTIObject part : object.<RTTIObject[]>get("Meshes")) {
            final var mesh = part.ref("Mesh").follow(core, packfile, registry);
            exportResource(mesh.binary(), mesh.object(), editor, file);
            break;
        }
    }

    private void exportMultiMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull PropertyEditor editor,
        @NotNull GltfFile file
    ) throws IOException {
        final var registry = editor.getInput().getProject().getTypeRegistry();
        final var packfile = editor.getInput().getNode().getPackfile();

        for (RTTIObject part : object.<RTTIObject[]>get("Parts")) {
            final var mesh = part.ref("Mesh").follow(core, packfile, registry);
            final var transform = part.obj("Transform");
            final var pos = transform.obj("Position");
            final var ori = transform.obj("Orientation");

            exportResource(mesh.binary(), mesh.object(), editor, file);

            file.nodes.get(file.nodes.size() - 1).matrix = new double[]{
                ori.obj("Col0").f32("X"), ori.obj("Col0").f32("Y"), ori.obj("Col0").f32("Z"), pos.f64("X"),
                ori.obj("Col1").f32("X"), ori.obj("Col1").f32("Y"), ori.obj("Col1").f32("Z"), pos.f64("Y"),
                ori.obj("Col2").f32("X"), ori.obj("Col2").f32("Y"), ori.obj("Col2").f32("Z"), pos.f64("Z"),
                0, 0, 0, 1
            };
        }
    }

    private void exportRegularSkinnedMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull PropertyEditor editor,
        @NotNull GltfFile file
    ) throws IOException {
        final var registry = editor.getInput().getProject().getTypeRegistry();
        final var packfile = editor.getInput().getNode().getPackfile();

        final RTTIObject dataSource = object.get("DataSource");
        final byte[] dataSourceData = packfile.extract("%s.core.stream".formatted(dataSource.<String>get("Location")));

        final GltfBuffer buffer = new GltfBuffer();
        buffer.uri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(dataSourceData);
        buffer.byteLength = dataSourceData.length;

        final GltfMesh mesh = new GltfMesh();

        int bufferOffset = 0;

        for (RTTIReference ref : object.<RTTIReference[]>get("Primitives")) {
            final var primitive = ref.follow(core, packfile, registry);
            final var vertexArray = primitive.object().ref("VertexArray").follow(primitive.binary(), packfile, registry).object();
            final RTTIObject vertexArrayData = vertexArray.get("Data");
            final int vertexCount = vertexArrayData.get("VertexCount");

            final GltfMesh.Primitive meshPrimitive = new GltfMesh.Primitive();
            mesh.primitives.add(meshPrimitive);

            for (RTTIObject stream : vertexArrayData.<RTTIObject[]>get("Streams")) {
                final int stride = stream.get("Stride");

                int offset = 0;

                for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
                    final ElementTypeDescriptor descriptor = ELEMENT_TYPES.get(element.get("Type").toString());

                    if (descriptor == null) {
                        continue;
                    }

                    meshPrimitive.attributes.put(descriptor.name(), file.accessors.size());

                    final GltfAccessor accessor = new GltfAccessor();
                    accessor.bufferView = file.bufferViews.size();
                    accessor.count = vertexCount;
                    accessor.componentType = descriptor.componentType();
                    accessor.type = descriptor.accessorType();

                    final GltfBufferView view = new GltfBufferView();
                    view.buffer = file.buffers.size();
                    view.byteOffset = bufferOffset + offset;
                    view.byteLength = vertexCount * stride - stride + descriptor.size();
                    view.byteStride = stride;

                    file.accessors.add(accessor);
                    file.bufferViews.add(view);

                    offset += element.i8("Offset");
                }

                bufferOffset += IOUtils.alignUp(vertexCount * stride, 256);
            }

            final var indexArray = primitive.object().ref("IndexArray").follow(primitive.binary(), packfile, registry).object();
            final var indexArrayData = indexArray.obj("Data");
            final var indexCount = indexArrayData.i32("IndexCount");

            assert indexArrayData.get("Format").toString().equals("Index16");

            final GltfAccessor accessor = new GltfAccessor();
            accessor.bufferView = file.bufferViews.size();
            accessor.count = indexCount;
            accessor.componentType = 5123;
            accessor.type = "SCALAR";

            final GltfBufferView view = new GltfBufferView();
            view.buffer = file.buffers.size();
            view.byteOffset = bufferOffset;
            view.byteLength = indexCount * Short.BYTES;

            bufferOffset += IOUtils.alignUp(indexCount * Short.BYTES, 256);

            meshPrimitive.indices = file.accessors.size();
            file.accessors.add(accessor);
            file.bufferViews.add(view);
        }

        final GltfNode node = new GltfNode();
        node.mesh = file.meshes.size();

        file.scenes.get(file.scenes.size() - 1).nodes.add(file.nodes.size());
        file.nodes.add(node);
        file.buffers.add(buffer);
        file.meshes.add(mesh);
    }

    private static record ElementTypeDescriptor(@NotNull String name, int slots, int size, int componentType, @NotNull String accessorType) {}
}
