package com.shade.decima.ui.data.viewer.mesh;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializer;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.packfile.PackfileManager;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.controls.LabeledBorder;
import com.shade.decima.ui.data.viewer.mesh.data.ComponentType;
import com.shade.decima.ui.data.viewer.mesh.data.ElementType;
import com.shade.decima.ui.data.viewer.mesh.data.StorageType;
import com.shade.decima.ui.data.viewer.mesh.dmf.*;
import com.shade.decima.ui.data.viewer.mesh.utils.MathUtils;
import com.shade.decima.ui.data.viewer.mesh.utils.Quaternion;
import com.shade.decima.ui.data.viewer.mesh.utils.Transform;
import com.shade.decima.ui.data.viewer.mesh.utils.Vector3;
import com.shade.decima.ui.data.viewer.texture.TextureViewer;
import com.shade.decima.ui.data.viewer.texture.controls.ImageProvider;
import com.shade.decima.ui.data.viewer.texture.exporter.TextureExporterDDS;
import com.shade.decima.ui.data.viewer.texture.reader.ImageReaderProvider;
import com.shade.decima.ui.editor.core.CoreEditor;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.shade.decima.ui.data.viewer.texture.TextureViewer.getImageReaderProvider;

public class MeshViewerPanel extends JComponent {
    private static final Logger log = LoggerFactory.getLogger(MeshViewerPanel.class);
    private static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(List.class, (JsonSerializer<List<?>>) (src, type, context) -> {
            if (src == null || src.isEmpty()) {
                return null;
            }
            final JsonArray result = new JsonArray();
            for (Object o : src) {
                result.add(context.serialize(o));
            }
            return result;
        })
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();


    private static final Map<String, AccessorDescriptor> SEMANTIC_DESCRIPTORS = Map.ofEntries(
        Map.entry("Pos", new AccessorDescriptor("POSITION", ElementType.VEC3, ComponentType.FLOAT, false, false)),
        Map.entry("TangentBFlip", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Tangent", new AccessorDescriptor("TANGENT", ElementType.VEC4, ComponentType.FLOAT, false, true)),
        Map.entry("Normal", new AccessorDescriptor("NORMAL", ElementType.VEC3, ComponentType.FLOAT, false, true)),
        Map.entry("Color", new AccessorDescriptor("COLOR_0", ElementType.VEC4, ComponentType.BYTE, true, true)),
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


    private final JButton exportButton;
    //    private final JCheckBox convertVerticesCheckBox;
    private final JCheckBox embeddedBuffersCheckBox;
    private CoreEditor editor;

    public MeshViewerPanel() {
        final JLabel placeholder = new JLabel("Preview is not supported");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");

        exportButton = new JButton("Export\u2026");
        exportButton.setEnabled(false);
        exportButton.addActionListener(event -> {
            final JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(editor.getInput().getName() + ".dmf"));
            chooser.setDialogTitle("Choose output file");
            chooser.setFileFilter(new FileExtensionFilter("Decima Model File", "dmf"));
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

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new LabeledBorder("Export settings"));
//        settingsPanel.add(convertVerticesCheckBox = new JCheckBox("Convert vertices", false));
        settingsPanel.add(embeddedBuffersCheckBox = new JCheckBox("Embed buffers", true));

        setLayout(new MigLayout("ins panel", "[grow,fill]", "[grow,fill][][]"));
        add(placeholder, "wrap");
        add(settingsPanel, "wrap");
        add(exportButton);
    }

    public void setInput(@Nullable CoreEditor editor) {
        this.editor = editor;
        this.exportButton.setEnabled(editor != null);
    }

    private void export(@NotNull Path output) throws IOException {
        final var object = (RTTIObject) Objects.requireNonNull(editor.getSelectedValue());
        String filename = output.getFileName().toString();
        Path outputDir = output.getParent().resolve("dbuffers");
        Files.createDirectories(outputDir);

        String resourceName = filename.substring(0, filename.indexOf('.'));
        ModelExportContext context = new ModelExportContext(resourceName, outputDir, new DMFSceneFile(1));
        context.embedBuffers = embeddedBuffersCheckBox.isSelected();
//        context.convertVertices = convertVerticesCheckBox.isSelected();
        exportResource(editor.getCoreBinary(), object, editor.getInput().getProject(), context, resourceName);

        Files.writeString(output, GSON.toJson(context.scene));
    }

    private static void exportResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        log.info("Exporting {} to mesh", object.getType().getTypeName());
        switch (object.getType().getTypeName()) {
            case "ArtPartsDataResource" -> exportArtPartsDataResource(core, object, project, context, resourceName);
            case "ObjectCollection" -> exportObjectCollection(core, object, project, context);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(core, object, project, context);
//            case "Terrain" -> exportTerrainResource(core, object, project, context);
//            case "LodMeshResource" -> exportLodMeshResource(core, object, project, context);
            case "MultiMeshResource" -> exportMultiMeshResource(core, object, project, context, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                exportRegularSkinnedMeshResource(core, object, project, context, resourceName);
            default -> throw new IllegalArgumentException("Unsupported resource: " + object.getType());
        }
    }

    private static DMFNode toModel(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        context.depth += 1;
        log.info("{}Converting {} to mesh", "\t".repeat(context.depth), object.getType().getTypeName());
        var res = switch (object.getType().getTypeName()) {
            case "PrefabResource" -> prefabResourceToModel(core, object, project, context, resourceName);
            case "ModelPartResource" -> modelPartResourceToModel(core, object, project, context, resourceName);
            case "ArtPartsSubModelWithChildrenResource" ->
                artPartsSubModelWithChildrenResourceToModel(core, object, project, context, resourceName);
            case "ArtPartsSubModelResource" ->
                artPartsSubModelResourceToModel(core, object, project, context, resourceName);
            case "PrefabInstance" -> prefabInstanceToModel(core, object, project, context, resourceName);
            case "ObjectCollection" -> objectCollectionToModel(core, object, project, context, resourceName);
            case "StaticMeshInstance" -> staticMeshInstanceToModel(core, object, project, context, resourceName);
//            case "Terrain" -> terrainResourceToModel(core, object, project, context);
            case "LodMeshResource" -> lodMeshResourceToModel(core, object, project, context, resourceName);
            case "MultiMeshResource" -> multiMeshResourceToModel(core, object, project, context, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                regularSkinnedMeshResourceToModel(core, object, project, context, resourceName);
            default -> {
                log.info("{}Cannot export {}", "\t".repeat(context.depth), object.getType().getTypeName());
                yield null;
            }
        };
        context.depth -= 1;
        return res;
    }


    private static void exportArtPartsDataResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);

        DMFSkeleton skeleton = new DMFSkeleton();
        context.scene.skeletons.add(skeleton);
        skeleton.transform = DMFTransform.FromTransform(transform);


        RTTIObject representationSkeleton = object.ref("RepresentationSkeleton").follow(core, project.getPackfileManager(), project.getTypeRegistry()).object();
        RTTIObject[] defaultPoseRotations = object.get("DefaultPoseRotations");
        RTTIObject[] defaultPoseTranslations = object.get("DefaultPoseTranslations");

        RTTIObject[] joints = representationSkeleton.get("Joints");

        for (int i = 0; i < joints.length; i++) {
            RTTIObject joint = joints[i];
            int parentId = joint.i16("ParentIndex");
            RTTIObject translation = defaultPoseTranslations[i];
            Quaternion rot;
            if (defaultPoseRotations.length > 0) {
                RTTIObject rotation = defaultPoseRotations[i];
                rot = new Quaternion(rotation.f32("X"), rotation.f32("Y"), rotation.f32("Z"), rotation.f32("W"));

            } else {
                rot = new Quaternion(0, 0, 0, 1);
            }

            Vector3 pos = new Vector3(translation.f32("X"), translation.f32("Y"), translation.f32("Z"));

            skeleton.newBone(joint.str("Name"), new DMFTransform(pos, new Vector3(1, 1, 1), rot), parentId);
        }


        RTTIReference.FollowResult rootModelRes = object.ref("RootModel").follow(core, project.getPackfileManager(), project.getTypeRegistry());
        DMFModel model = (DMFModel) toModel(rootModelRes.binary(), rootModelRes.object(), project, context, resourceName);
        model.children = new ArrayList<>();
        RTTIReference[] get = object.get("SubModelPartResources");
        for (int i = 0; i < get.length; i++) {
            RTTIReference subPart = get[i];
            RTTIReference.FollowResult subPartRes = subPart.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            model.children.add(toModel(subPartRes.binary(), subPartRes.object(), project, context, "SubModel%d_%s".formatted(i, nameFromReference(subPart, resourceName))));
        }
        model.setSkeleton(skeleton, context.scene);
        context.scene.models.add(model);
    }

    private static void exportMultiMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {

        DMFModelGroup group = new DMFModelGroup();
        group.name = "Root";
        group.transform = DMFTransform.FromTransform(Transform.fromRotation(0, -90, 0));
        context.scene.models.add(group);
        DMFNode node = toModel(core, object, project, context, resourceName);
        group.children.add(node);
    }

    private static void exportObjectCollection(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);
        DMFModelGroup group = new DMFModelGroup();
        group.name = context.resourceName;
//        group.transform = DMFTransform.FromTransform(transform);
        context.scene.models.add(group);
        int itemId = 0;
        RTTIReference[] objects = object.get("Objects");
        for (RTTIReference rttiReference : objects) {
            RTTIReference.FollowResult refObject = rttiReference.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            DMFNode node = toModel(refObject.binary(), refObject.object(), project, context, nameFromReference(rttiReference, "%s_Object_%d".formatted(context.resourceName, itemId)));
            group.children.add(node);
            itemId++;
        }
    }

    private static DMFNode artPartsSubModelResourceToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        DMFNode model;

        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            model = toModel(meshResourceRes.binary(), meshResourceRes.object(), project, context, resourceName);
        } else {
            model = new DMFModelGroup();
            model.name = resourceName;
        }
        RTTIReference extraMeshResourceRef = object.ref("ExtraResource");
        if (extraMeshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult extraMeshResourceRes = extraMeshResourceRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            DMFNode extraModel = toModel(extraMeshResourceRes.binary(), extraMeshResourceRes.object(), project, context, "EXTRA_" + nameFromReference(extraMeshResourceRef, resourceName));
            if (extraModel != null) {
                model.children.add(extraModel);
            }
        }
        return model;
    }

    private static DMFNode artPartsSubModelWithChildrenResourceToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("ArtPartsSubModelPartResource");
        DMFNode model;

        if (meshResourceRef.type() != RTTIReference.Type.NONE) {
            RTTIReference.FollowResult meshResourceRes = meshResourceRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            model = toModel(meshResourceRes.binary(), meshResourceRes.object(), project, context, nameFromReference(meshResourceRef, resourceName));
        } else {
            model = new DMFModelGroup();
            model.name = resourceName;
        }
        model.children = new ArrayList<>();
        RTTIReference[] get = object.get("Children");
        for (int i = 0; i < get.length; i++) {
            RTTIReference subPart = get[i];
            RTTIReference.FollowResult subPartRes = subPart.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            model.children.add(toModel(subPartRes.binary(), subPartRes.object(), project, context, nameFromReference(subPart, "child%d_%s".formatted(i, resourceName))));
        }
        return model;
    }

    private static DMFNode modelPartResourceToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
    ) throws IOException {
        RTTIReference meshResourceRef = object.ref("MeshResource");
        RTTIReference.FollowResult meshResource = meshResourceRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
        return toModel(meshResource.binary(), meshResource.object(), project, context, nameFromReference(meshResourceRef, resourceName));
    }

    private static DMFNode prefabResourceToModel(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference objectCollection = object.ref("ObjectCollection");
        RTTIReference.FollowResult prefabResource = objectCollection.follow(core, project.getPackfileManager(), project.getTypeRegistry());
        return toModel(prefabResource.binary(), prefabResource.object(), project, context, nameFromReference(objectCollection, resourceName));
    }

    private static DMFNode prefabInstanceToModel(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference prefab = object.ref("Prefab");
        RTTIReference.FollowResult prefabResource = prefab.follow(core, project.getPackfileManager(), project.getTypeRegistry());
        DMFNode node = toModel(prefabResource.binary(), prefabResource.object(), project, context, nameFromReference(prefab, resourceName));
        if (node == null) {
            return null;
        }
        if (node.transform != null) {
            throw new IllegalStateException("Unexpected transform");
        }
        Transform transform = worldTransformToMatrix(object.get("Orientation"));
        node.transform = DMFTransform.FromTransform(transform);
        return node;
    }

    private static DMFNode staticMeshInstanceToModel(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        RTTIReference resource = object.ref("Resource");
        RTTIReference.FollowResult meshResource = resource.follow(core, project.getPackfileManager(), project.getTypeRegistry());
        return toModel(meshResource.binary(), meshResource.object(), project, context, nameFromReference(resource, resourceName));
    }
//
//    private static void exportStaticMeshInstance(
//        @NotNull CoreBinary core,
//        @NotNull RTTIObject object,
//        @NotNull Project project,
//        @NotNull ModelExportContext context
//    ) throws IOException {
//        GltfFile file = context.file;
//        RTTIReference.FollowResult meshResource = object.ref("Resource").follow(core, project.getPackfileManager(), project.getTypeRegistry());
//        GltfScene scene = new GltfScene(file);
//        List<GltfNode> nodes = toModel(meshResource.binary(), meshResource.object(), project, context, context.resourceName);
//        nodes = addTransformOrWrap(nodes, Transform.fromRotation(0, -90, 0), file);
//        for (GltfNode node : nodes) {
//            scene.addNode(node, file);
//        }
//    }
//
//


    private static DMFNode objectCollectionToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        ModelExportContext context,
        String resourceName
    ) throws IOException {
        RTTIReference[] objects = object.get("Objects");
        DMFModelGroup group = new DMFModelGroup();
        group.name = "Collection %s".formatted(resourceName);
        int itemId = 0;
        for (RTTIReference rttiReference : objects) {
            RTTIReference.FollowResult refObject = rttiReference.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            DMFNode node = toModel(refObject.binary(), refObject.object(), project, context, "%s_Object_%d".formatted(nameFromReference(rttiReference, resourceName), itemId));
            itemId++;
            if (node == null) {
                continue;
            }
            group.children.add(node);
        }
        return group;
    }

    //    private static void exportLodMeshResource(
//        @NotNull CoreBinary core,
//        @NotNull RTTIObject object,
//        @NotNull Project project,
//        @NotNull ModelExportContext context
//    ) throws IOException {
//        GltfFile file = context.file;
//        GltfScene scene = new GltfScene(file);
//        GltfNode rootNode = new GltfNode(file, "SceneRoot");
//        Transform transform = Transform.fromRotation(0, -90, 0);
//        rootNode.translation = transform.getTranslation();
//        rootNode.scale = transform.getScale();
//        rootNode.rotation = transform.getRotation();
//        scene.addNode(rootNode, file);
//        List<GltfNode> gltfNodes = toModel(core, object, project, context, context.resourceName);
//        for (GltfNode node : gltfNodes) {
//            rootNode.addNode(node, file);
//        }
//    }
//
    private static DMFNode lodMeshResourceToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
    ) throws IOException {
        RTTIObject[] meshes = object.get("Meshes");
        if (meshes.length == 0) {
            return null;
        }
        RTTIObject lod = meshes[0];
        RTTIReference meshRef = lod.ref("Mesh");
        final var mesh = meshRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
        return toModel(mesh.binary(), mesh.object(), project, context, "%s_LOD%d".formatted(nameFromReference(meshRef, resourceName), 0));

    }


    private static DMFNode multiMeshResourceToModel(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
    ) throws IOException {
        DMFModelGroup group = new DMFModelGroup();
        RTTIObject[] parts = object.get("Parts");
        for (int partId = 0; partId < parts.length; partId++) {
            RTTIObject part = parts[partId];
            RTTIReference meshRef = part.ref("Mesh");
            final var mesh = meshRef.follow(core, project.getPackfileManager(), project.getTypeRegistry());
            Transform transform = worldTransformToMatrix(part.obj("Transform"));
            DMFNode model = toModel(mesh.binary(), mesh.object(), project, context, "%s_Part%d".formatted(nameFromReference(meshRef, resourceName), partId));
            if (model == null) continue;
            if (model.transform != null) {
                throw new IllegalStateException("Model already had transforms, please handle me!");
            }
            model.transform = DMFTransform.FromTransform(transform);
            group.children.add(model);
        }
        return group;
    }


    private static void exportRegularSkinnedMeshResource(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        Transform transform = Transform.fromRotation(0, -90, 0);

        DMFModel model = regularSkinnedMeshResourceToModel(core, object, project, context, resourceName);
        if (model != null) {
            model.transform = DMFTransform.FromTransform(transform);
            context.scene.models.add(model);
        }
    }

    private static DMFModel regularSkinnedMeshResourceToModel(
        @NotNull CoreBinary core,
        @NotNull RTTIObject object,
        @NotNull Project project,
        @NotNull ModelExportContext context,
        @NotNull String resourceName
    ) throws IOException {
        final var registry = project.getTypeRegistry();
        final var manager = project.getPackfileManager();
        DrawFlags flags = DrawFlags.fromDataAndRegistry(object.obj("DrawFlags").i32("Data"), project.getTypeRegistry());
        if (!flags.renderType().equals("Normal")) {
            return null;
        }
        final String dataSourceLocation = "%s.core.stream".formatted(object.obj("DataSource").str("Location"));
        final Packfile dataSourcePackfile = Objects.requireNonNull(manager.findAny(dataSourceLocation), "Can't find referenced data source");
        final ByteBuffer dataSource = ByteBuffer
            .wrap(dataSourcePackfile.extract(dataSourceLocation))
            .order(ByteOrder.LITTLE_ENDIAN);
        DMFModel model = new DMFModel();
        DMFMesh mesh;
        if (context.convertVertices) {
            MeshConverter modelConverter = new MeshConverter(core, object, project, dataSource, context, resourceName);
            mesh = modelConverter.processMesh();
        } else {
            DMFBuffer buffer;
            if (context.embedBuffers) {
                buffer = new DMFInternalBuffer(dataSource);
            } else {
                String bufferFileName = "%s.dbuf".formatted(resourceName);
                buffer = new DMFExternalBuffer(bufferFileName, dataSource.remaining());
                Files.write(context.outputDir.resolve(bufferFileName), dataSource.array());
            }
            buffer.originalName = dataSourceLocation;
            Map<RTTIObject, Map.Entry<Integer, Integer>> bufferOffsets = new HashMap<>();

            RTTIReference[] primitivesRefs = object.get("Primitives");
            RTTIReference[] shadingGroupsRefs = object.get("ShadingGroups");
            if (primitivesRefs.length != shadingGroupsRefs.length) {
                throw new IllegalStateException("Primitives count does not match ShadingGroups count!");
            }
            int dataSourceOffset = 0;
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


            }


            mesh = new DMFMesh();

            for (int i = 0; i < primitivesRefs.length; i++) {
                RTTIReference primitivesRef = primitivesRefs[i];
                RTTIReference shadingGroupRef = shadingGroupsRefs[i];
                final var primitiveRes = primitivesRef.follow(core, manager, registry);
                RTTIObject primitiveObj = primitiveRes.object();
                RTTIObject shadingGroupObj = shadingGroupRef.follow(core, manager, registry).object();
                RTTIObject vertexArray = primitiveObj.ref("VertexArray").follow(primitiveRes.binary(), manager, registry).object();
                RTTIObject indexArray = primitiveObj.ref("IndexArray").follow(primitiveRes.binary(), manager, registry).object();
                RTTIObject vertexArrayUUID = vertexArray.get("ObjectUUID");
                RTTIObject indicesArrayUUID = indexArray.get("ObjectUUID");

                final var vertices = vertexArray.obj("Data");
                final var indices = indexArray.obj("Data");

                final int vertexCount = vertices.i32("VertexCount");
                final int indexCount = indices.i32("IndexCount");
                final int indexStartIndex = primitiveObj.i32("StartIndex");
                final int indexEndIndex = primitiveObj.i32("EndIndex");
                final DMFPrimitive primitive = mesh.newPrimitive();
                primitive.vertexCount = vertexCount;
                primitive.vertexType = DMFVertexType.SINGLEBUFFER;
                primitive.vertexStart = 0;
                primitive.vertexEnd = vertexCount;
                Map.Entry<Integer, Integer> offsetAndGroupId = bufferOffsets.get(vertexArrayUUID);
                dataSourceOffset = offsetAndGroupId.getKey();
                for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
                    final int stride = stream.i32("Stride");
                    DMFBufferView bufferView = new DMFBufferView();

                    bufferView.offset = dataSourceOffset;
                    bufferView.size = stride * vertexCount;
                    bufferView.setBuffer(buffer, context.scene);
                    RTTIObject[] elements = stream.get("Elements");
                    for (int j = 0; j < elements.length; j++) {
                        RTTIObject element = elements[j];
                        final int offset = element.i8("Offset");
                        int realElementSize = 0;
                        if (j < elements.length - 1) {
                            realElementSize = elements[j + 1].i8("Offset") - offset;
                        } else if (j == 0) {
                            realElementSize = stride;
                        } else if (j == elements.length - 1) {
                            realElementSize = stride - offset;
                        }
                        String elementType = element.str("Type");
                        final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementType);
                        DMFVertexAttribute attribute = new DMFVertexAttribute();
                        StorageType storageType = StorageType.fromString(element.str("StorageType"));
                        attribute.offset = offset;
                        attribute.semantic = descriptor.semantic;
                        attribute.size = realElementSize;
                        attribute.elementType = storageType.getTypeName();
                        attribute.elementCount = realElementSize / storageType.getSize();
                        attribute.stride = stride;
                        attribute.setBufferView(bufferView, context.scene);
                        primitive.vertexAttributes.put(descriptor.semantic, attribute);
                    }
                    dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
                }
                int indexSize = switch (indices.str("Format")) {
                    case "Index16" -> 2;
                    case "Index32" -> 4;
                    default -> throw new IllegalStateException("Unexpected value: " + indices.str("Format"));
                };
                primitive.indexSize = indexSize;
                primitive.indexCount = indexCount;
                primitive.indexStart = indexStartIndex;
                primitive.indexEnd = indexEndIndex;
                DMFBufferView bufferView = new DMFBufferView();
                offsetAndGroupId = bufferOffsets.get(indicesArrayUUID);
                bufferView.offset = offsetAndGroupId.getKey();
                primitive.groupingId = offsetAndGroupId.getValue();
                bufferView.size = indexSize * indexCount;
                bufferView.setBuffer(buffer, context.scene);
                primitive.setIndexBufferView(bufferView, context.scene);

                RTTIObject materialUUID = shadingGroupObj.get("ObjectUUID");
                String materialName = uuidToString(materialUUID);
                DMFMaterial material;
                if (context.scene.getMaterial(materialName) == null) {
                    material = context.scene.createMaterial(materialName);
                    exportMaterial(shadingGroupObj, material, context.scene, core, manager, registry);
                } else {
                    material = context.scene.getMaterial(materialName);
                }
                primitive.setMaterial(material, context.scene);

            }
        }
        model.name = resourceName;
        model.mesh = mesh;
        return model;
    }

    private static String nameFromReference(@NotNull RTTIReference ref, @NotNull String resourceName) {
        if (ref.type() == RTTIReference.Type.EXTERNAL_LINK) {
            String path = ref.path();
            assert path != null;
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return resourceName;
    }

    @NotNull
    private static Transform worldTransformToMatrix(RTTIObject transformObj) {
        assert transformObj.getType().getTypeName().equals("WorldTransform");
        final var posObj = transformObj.obj("Position");
        final var oriObj = transformObj.obj("Orientation");
        final RTTIObject col0Obj = oriObj.obj("Col0");
        final RTTIObject col1Obj = oriObj.obj("Col1");
        final RTTIObject col2Obj = oriObj.obj("Col2");
        final double[] col0 = {col0Obj.f32("X"), col0Obj.f32("Y"), col0Obj.f32("Z")};
        final double[] col1 = {col1Obj.f32("X"), col1Obj.f32("Y"), col1Obj.f32("Z")};
        final double[] col2 = {col2Obj.f32("X"), col2Obj.f32("Y"), col2Obj.f32("Z")};
        double[] pos = {posObj.f64("X"), posObj.f64("Y"), posObj.f64("Z")};

        Transform transform = Transform.fromRotationAndScaleMatrix(new double[][]{col0, col1, col2});
        transform.setTranslation(pos);
        return transform;
    }

    private static String uuidToString(RTTIObject uuid) {
        return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".formatted(
            uuid.i8("Data3"),
            uuid.i8("Data2"),
            uuid.i8("Data1"),
            uuid.i8("Data0"),
            uuid.i8("Data5"),
            uuid.i8("Data4"),
            uuid.i8("Data7"),
            uuid.i8("Data6"),
            uuid.i8("Data8"),
            uuid.i8("Data9"),
            uuid.i8("Data10"),
            uuid.i8("Data11"),
            uuid.i8("Data12"),
            uuid.i8("Data13"),
            uuid.i8("Data14"),
            uuid.i8("Data15")
        );
    }

    private static List<String> supportedRenderTechniqueTypes = List.of(
        "Deferred"
    );

    private static void exportMaterial(RTTIObject shadingGroup, DMFMaterial material, DMFSceneFile scene, CoreBinary binary, PackfileManager manager, RTTITypeRegistry registry) throws IOException {
        RTTIReference renderEffectRef = shadingGroup.ref("RenderEffect");
        if (renderEffectRef.type() == RTTIReference.Type.NONE) {
            return;
        }
        RTTIReference.FollowResult renderEffectRes = renderEffectRef.follow(binary, manager, registry);
        RTTIObject renderEffect = renderEffectRes.object();
//        if (!renderEffect.str("EffectType").equals("Object render effect")) {
//            return;
//        }
        int textureId = 0;
        for (RTTIObject techniqueSet : renderEffect.<RTTIObject[]>get("TechniqueSets")) {
            for (RTTIObject renderTechnique : techniqueSet.<RTTIObject[]>get("RenderTechniques")) {
                if (/*supportedRenderTechniqueTypes.contains(renderTechnique.str("TechniqueType"))*/true) {
                    RTTIObject[] get = renderTechnique.<RTTIObject[]>get("TextureBindings");
                    for (RTTIObject textureBinding : get) {
                        RTTIReference textureRef = textureBinding.ref("TextureResource");
                        if (textureRef.type() == RTTIReference.Type.NONE) {
                            continue;
                        }
                        RTTIReference.FollowResult textureRes = textureRef.follow(binary, manager, registry);
                        RTTIObject texture = textureRes.object();
                        if (texture.getType().getTypeName().equals("Texture")) {
                            if (exportTexture(material, scene, manager, textureId, texture)) {
                                textureId++;
                            }

                        } else if (texture.getType().getTypeName().equals("TextureSet")) {
                            for (RTTIObject entry : texture.<RTTIObject[]>get("Entries")) {
                                RTTIReference textureSetTextureRef = entry.ref("Texture");
                                if (textureSetTextureRef.type() == RTTIReference.Type.NONE) {
                                    continue;
                                }
                                RTTIObject textureSetTexture = textureSetTextureRef.follow(textureRes.binary(), manager, registry).object();
                                if (exportTexture(material, scene, manager, textureId, textureSetTexture)) {
                                    textureId++;
                                }
                            }
                        } else {
                            log.warn("Texture of type {} not supported", texture.getType().getTypeName());
                        }
                    }
                }
            }
        }
    }

    private static boolean exportTexture(DMFMaterial material, DMFSceneFile scene, PackfileManager manager, int textureId, RTTIObject texture) throws IOException {
        final String textureName = uuidToString(texture.get("ObjectUUID"));
        String textureUsageName = "Texture%d".formatted(textureId);
        if (scene.getTexture(textureName) != null) {
            material.textureIds.put(textureUsageName, scene.textures.indexOf(scene.getTexture(textureName)));
        } else {
            DMFTexture dmfTexture = scene.createTexture(textureName);
            material.textureIds.put(textureUsageName, scene.textures.indexOf(dmfTexture));
            final RTTIObject header = texture.get("Header");
            final ImageReaderProvider imageReaderProvider = getImageReaderProvider(header.get("PixelFormat").toString());
            final ImageProvider imageProvider = imageReaderProvider != null ? new TextureViewer.MyImageProvider(texture, manager, imageReaderProvider) : null;
            if (imageProvider == null) {
                return false;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            new TextureExporterDDS().export(imageProvider, Set.of(), Channels.newChannel(stream));
            dmfTexture.dataType = DMFDataType.DDS;
            byte[] src = stream.toByteArray();
            dmfTexture.embeddedData = Base64.getEncoder().encodeToString(src);
            dmfTexture.embeddedDataSize = src.length;
        }
        return true;
    }

    private record AccessorDescriptor(@NotNull String semantic, @NotNull ElementType elementType, @NotNull ComponentType componentType, boolean unsigned, boolean normalized) {}


    private static class DrawFlags {
        public boolean castShadow;
        public String renderType;
        public String shadowCullMode;
        public String viewLayer;
        public float shadowBiasMultiplier;
        public String shadowBiasMode;
        public boolean disableOcclusionCulling;
        public boolean voxelizeLightBake;

        public DrawFlags() {
        }

        public static DrawFlags fromDataAndRegistry(int flags, RTTITypeRegistry registry) {
            RTTITypeEnum eDrawPartType = ((RTTITypeEnum) registry.find("EDrawPartType"));
            RTTITypeEnum eShadowCull = ((RTTITypeEnum) registry.find("EShadowCull"));
            RTTITypeEnum eViewLayer = ((RTTITypeEnum) registry.find("EViewLayer"));
            RTTITypeEnum eShadowBiasMode = ((RTTITypeEnum) registry.find("EShadowBiasMode"));

            DrawFlags drawFlags = new DrawFlags();
            drawFlags.castShadow = (flags & 1) > 0;
            drawFlags.renderType = eDrawPartType.valueOf((flags >>> 3) & 1).name();
            drawFlags.shadowCullMode = eShadowCull.valueOf((flags >>> 1) & 3).name();
            drawFlags.viewLayer = eViewLayer.valueOf((flags >>> 4) & 3).name();
            drawFlags.shadowBiasMultiplier = MathUtils.toFloat((short) ((flags >>> 6) & 65535));
            drawFlags.shadowBiasMode = eShadowBiasMode.valueOf((flags >>> 22) & 1).name();
            drawFlags.disableOcclusionCulling = ((flags >>> 24) & 1) > 0;
            drawFlags.voxelizeLightBake = (flags & 0x2000000) > 0;
            return drawFlags;
        }

        public boolean castShadow() {
            return castShadow;
        }

        public String renderType() {
            return renderType;
        }

        public String shadowCullMode() {
            return shadowCullMode;
        }

        public String viewLayer() {
            return viewLayer;
        }

        public float shadowBiasMultiplier() {
            return shadowBiasMultiplier;
        }

        public String shadowBiasMode() {
            return shadowBiasMode;
        }

        public boolean disableOcclusionCulling() {
            return disableOcclusionCulling;
        }

        public boolean voxelizeLightBake() {
            return voxelizeLightBake;
        }

    }


}
