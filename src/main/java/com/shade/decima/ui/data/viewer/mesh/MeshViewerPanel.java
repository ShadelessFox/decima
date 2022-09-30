package com.shade.decima.ui.data.viewer.mesh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializer;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.objects.RTTIReference;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.ui.Application;
import com.shade.decima.ui.controls.FileExtensionFilter;
import com.shade.decima.ui.data.viewer.mesh.data.*;
import com.shade.decima.ui.data.viewer.mesh.dmf.*;
import com.shade.decima.ui.data.viewer.mesh.utils.MathUtils;
import com.shade.decima.ui.data.viewer.mesh.utils.Quaternion;
import com.shade.decima.ui.data.viewer.mesh.utils.Transform;
import com.shade.decima.ui.data.viewer.mesh.utils.Vector3;
import com.shade.decima.ui.editor.property.PropertyEditor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MeshViewerPanel extends JComponent {
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
    private PropertyEditor editor;

    public MeshViewerPanel() {
        final JLabel placeholder = UIUtils.Labels.h1("Preview is not supported");
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);

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
        String filename = output.getFileName().toString();
        Path outputDir = output.getParent().resolve("dbuffers");
        File tmp = new File(outputDir.toUri());
        if (!tmp.exists()) {
            tmp.mkdirs();
        }

        String resourceName = filename.substring(0, filename.indexOf('.') - 1);
        ModelExportContext context = new ModelExportContext(resourceName, outputDir, new DMFSceneFile("Decima Explorer", 1));

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
        switch (object.getType().getTypeName()) {
            case "ArtPartsDataResource" -> exportArtPartsDataResource(core, object, project, context, resourceName);
//            case "ObjectCollection" -> exportObjectCollection(core, object, project, context);
//            case "StaticMeshInstance" -> exportStaticMeshInstance(core, object, project, context);
//            case "Terrain" -> exportTerrainResource(core, object, project, context);
//            case "LodMeshResource" -> exportLodMeshResource(core, object, project, context);
//            case "MultiMeshResource" -> exportMultiMeshResource(core, object, project, context);
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
        return switch (object.getType().getTypeName()) {
//            case "PrefabResource" -> prefabResourceToModel(core, object, project, scene, resourceName);
            case "ModelPartResource" -> modelPartResourceToModel(core, object, project, context, resourceName);
            case "ArtPartsSubModelWithChildrenResource" ->
                artPartsSubModelWithChildrenResourceToModel(core, object, project, context, resourceName);
            case "ArtPartsSubModelResource" ->
                artPartsSubModelResourceToModel(core, object, project, context, resourceName);
//            case "PrefabInstance" -> prefabInstanceToModel(core, object, project, scene, resourceName);
//            case "ObjectCollection" -> objectCollectionToModel(core, object, project, scene, resourceName);
//            case "StaticMeshInstance" -> staticMeshInstanceToModel(core, object, project, scene, resourceName);
//            case "Terrain" -> terrainResourceToModel(core, object, project, context);
            case "LodMeshResource" -> lodMeshResourceToModel(core, object, project, context, resourceName);
            case "MultiMeshResource" -> multiMeshResourceToModel(core, object, project, context, resourceName);
            case "RegularSkinnedMeshResource", "StaticMeshResource" ->
                regularSkinnedMeshResourceToModel(core, object, project, context, resourceName);
            default -> null;
        };
    }


    private static void exportArtPartsDataResource(
        CoreBinary core,
        RTTIObject object,
        Project project,
        @NotNull ModelExportContext context,
        String resourceName
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
            RTTIObject rotation = defaultPoseRotations[i];
            RTTIObject translation = defaultPoseTranslations[i];

            Vector3 pos = new Vector3(translation.f32("X"), translation.f32("Y"), translation.f32("Z"));
            Quaternion rot = new Quaternion(rotation.f32("X"), rotation.f32("Y"), rotation.f32("Z"), rotation.f32("W"));

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
            model.children.add(toModel(subPartRes.binary(), subPartRes.object(), project, context, "CHILD%d_%s".formatted(i, nameFromReference(subPart, resourceName))));
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

//    private static List<GltfNode> prefabResourceToModel(
//        @NotNull CoreBinary core,
//        @NotNull RTTIObject object,
//        @NotNull Project project,
//        @NotNull ModelExportContext context,
//        @NotNull String resourceName
//    ) throws IOException {
//        RTTIReference.FollowResult prefabResource = object.ref("ObjectCollection").follow(core, project.getPackfileManager(), project.getTypeRegistry());
//        return toModel(prefabResource.binary(), prefabResource.object(), project, context, resourceName);
//    }

    //    private static List<GltfNode> prefabInstanceToModel(
//        @NotNull CoreBinary core,
//        @NotNull RTTIObject object,
//        @NotNull Project project,
//        @NotNull ModelExportContext context,
//        @NotNull String resourceName
//    ) throws IOException {
//        RTTIReference.FollowResult prefabResource = object.ref("Prefab").follow(core, project.getPackfileManager(), project.getTypeRegistry());
//        Transform transform = worldTransformToMatrix(object.get("Orientation"));
//        List<GltfNode> nodes = toModel(prefabResource.binary(), prefabResource.object(), project, context, resourceName);
//        nodes = addTransformOrWrap(nodes, transform, context.file);
//        return nodes;
//    }
//
//    private static List<GltfNode> staticMeshInstanceToModel(
//        @NotNull CoreBinary core,
//        @NotNull RTTIObject object,
//        @NotNull Project project,
//        @NotNull ModelExportContext context,
//        @NotNull String resourceName
//    ) throws IOException {
//        RTTIReference.FollowResult meshResource = object.ref("Resource").follow(core, project.getPackfileManager(), project.getTypeRegistry());
//        return toModel(meshResource.binary(), meshResource.object(), project, context, resourceName);
//    }
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
//    private static void exportObjectCollection(
//        CoreBinary core,
//        RTTIObject object,
//        Project project,
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
//        List<GltfNode> meshes = toModel(core, object, project, context, context.resourceName);
//        for (GltfNode node : meshes) {
//            rootNode.addNode(node, file);
//        }
//    }
//
//    private static List<GltfNode> objectCollectionToModel(
//        CoreBinary core,
//        RTTIObject object,
//        Project project,
//        ModelExportContext context,
//        String resourceName
//    ) throws IOException {
//        GltfFile file = context.file;
//        RTTIReference[] objects = object.get("Objects");
//        GltfNode rootNode = new GltfNode(file);
//        rootNode.name = "Collection %s".formatted(resourceName);
//        int itemId = 0;
//        for (RTTIReference rttiReference : objects) {
//            RTTIReference.FollowResult refObject = rttiReference.follow(core, project.getPackfileManager(), project.getTypeRegistry());
//            List<GltfNode> nodes = toModel(refObject.binary(), refObject.object(), project, context, "Object_%d".formatted(itemId));
//            for (GltfNode node : nodes) {
//                rootNode.addNode(node, file);
//            }
//            itemId++;
//        }
//        return List.of(rootNode);
//    }
//
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

    //    private static void exportMultiMeshResource(
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
//        List<GltfNode> nodes = toModel(core, object, project, context, context.resourceName);
//        for (GltfNode node : nodes) {
//            rootNode.addNode(node, file);
//        }
//
//    }
//
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

        MeshConverter modelConverter = new MeshConverter(core, object, project, dataSource, context, resourceName);
        DMFMesh mesh = modelConverter.processMesh();
        DMFModel model = new DMFModel();
        model.name = resourceName;
        model.mesh = mesh;
        return model;

//        final GltfMesh gltfMesh = new GltfMesh(file);
//        gltfMesh.name = resourceName;
//        GltfNode meshNode = new GltfNode(file, gltfMesh);
//        meshNode.name = resourceName;
//
//        if (context.currentSkin != null) {
//            meshNode.setSkin(context.currentSkin, file);
//        }
//        int dataSourceOffset = 0;
//
//        Map<RTTIObject, List<GltfBuffer>> vertexBuffers = new HashMap<>();
//        Map<RTTIObject, GltfBuffer> indexBuffers = new HashMap<>();
//
//        for (RTTIReference ref : object.<RTTIReference[]>get("Primitives")) {
//            final var primitive = ref.follow(core, project.getPackfileManager(), registry);
//            RTTIObject vertexArray = primitive.object().ref("VertexArray").follow(primitive.binary(), project.getPackfileManager(), registry).object();
//            RTTIObject indexArray = primitive.object().ref("IndexArray").follow(primitive.binary(), project.getPackfileManager(), registry).object();
//            final var vertices = vertexArray.obj("Data");
//            final var indices = indexArray.obj("Data");
//
//            final int vertexCount = vertices.i32("VertexCount");
//            final int indexCount = indices.i32("IndexCount");
//            final int indexStartIndex = primitive.object().i32("StartIndex");
//            final int indexEndIndex = primitive.object().i32("EndIndex");
//
//            final Map<String, AccessorData> attributes = new LinkedHashMap<>();
//
//            for (RTTIObject stream : vertices.<RTTIObject[]>get("Streams")) {
//                final int stride = stream.i32("Stride");
//
//                for (RTTIObject element : stream.<RTTIObject[]>get("Elements")) {
//                    final int offset = element.i8("Offset");
////                    final int slots = element.i8("UsedSlots");
//                    String elementType = element.str("Type");
//                    final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementType);
//                    if (descriptor == null) {
//                        throw new IllegalArgumentException("Unsupported element type: " + elementType);
//                    }
//                    if (descriptor.semantic.equals("TANGENT")) continue;
//                    final var accessor = switch (element.str("StorageType")) {
//                        case "UnsignedByte" ->
//                            new AccessorDataInt8(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
//                        case "UnsignedByteNormalized" ->
//                            new AccessorDataInt8(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
//                        case "SignedShort" ->
//                            new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, false);
//                        case "SignedShortNormalized" ->
//                            new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
//                        case "UnsignedShort" ->
//                            new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, false);
//                        case "UnsignedShortNormalized" ->
//                            new AccessorDataInt16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
//                        case "HalfFloat" ->
//                            new AccessorDataFloat16(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset);
//                        case "Float" ->
//                            new AccessorDataFloat32(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset);
//                        case "X10Y10Z10W2Normalized" ->
//                            new AccessorDataXYZ10W2(dataSource, descriptor.elementType, vertexCount, 0, stride, dataSourceOffset + offset, false, true);
//                        case "X10Y10Z10W2UNorm" ->
//                            new AccessorDataInt32(dataSource, ElementType.SCALAR, vertexCount, 0, stride, dataSourceOffset + offset, true, true);
//                        default ->
//                            throw new IllegalArgumentException("Unsupported component type: " + element.str("StorageType"));
//                    };
//
//                    attributes.put(elementType, accessor);
//                }
//
//                dataSourceOffset += IOUtils.alignUp(stride * vertexCount, 256);
//            }
//
//            // TODO STEPS:
//            //  1. Pick suitable accessors for writing data
//            //  2. Allocate an output buffer big enough for holding repacked data
//            //  3. Write data from accessors into the output buffer
//            //  4. Write index data into the output buffer
//            //  5. Create required glTF types respectively
//
//            final GltfMesh.Primitive meshPrimitives = new GltfMesh.Primitive(gltfMesh);
//            RTTIObject vertexArrayUUID = vertexArray.obj("ObjectUUID");
//            if (vertexBuffers.containsKey(vertexArrayUUID)) {
//                int attributeId = 0;
//                List<GltfBuffer> buffers = vertexBuffers.get(vertexArrayUUID);
//                for (String elementName : attributes.keySet()) {
//                    AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementName);
//
//                    GltfBuffer gltfBuffer = buffers.get(attributeId);
//                    final GltfBufferView gltfBufferView = new GltfBufferView(file, gltfBuffer);
//                    gltfBufferView.byteOffset = 0;
//                    gltfBufferView.byteLength = gltfBuffer.byteLength;
//
//                    final GltfAccessor gltfAccessor = new GltfAccessor(file, gltfBufferView);
//                    gltfAccessor.type = descriptor.elementType.name();
//                    gltfAccessor.componentType = descriptor.componentType.getId();
//                    gltfAccessor.normalized = descriptor.normalized();
//                    gltfAccessor.count = vertexCount;
//
//                    meshPrimitives.attributes.put(descriptor.semantic, file.accessors.indexOf(gltfAccessor));
//                    attributeId++;
//                }
//            } else {
//                List<GltfBuffer> elementBuffers = new ArrayList<>();
//                vertexBuffers.put(vertexArrayUUID, elementBuffers);
//                attributes.forEach((elementType, supplier) -> {
//                    final AccessorDescriptor descriptor = SEMANTIC_DESCRIPTORS.get(elementType);
//                    final int size = descriptor.elementType.getStride(descriptor.componentType) * vertexCount;
//                    final ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
//                    final AccessorData consumer = switch (descriptor.semantic) {
//                        case "POSITION", "NORMAL", "TANGENT",
//                            "TEXCOORD_0", "TEXCOORD_1", "TEXCOORD_2", "TEXCOORD_3", "TEXCOORD_4", "TEXCOORD_5", "TEXCOORD_6" ->
//                            new AccessorDataFloat32(buffer, descriptor.elementType, vertexCount, 0, 0, 0);
//                        case "COLOR_0", "WEIGHTS_0", "WEIGHTS_1", "WEIGHTS_2", "WEIGHTS_3" ->
//                            new AccessorDataInt8(buffer, descriptor.elementType, vertexCount, 0, 0, 0, true, true);
//                        case "JOINTS_0", "JOINTS_1", "JOINTS_2", "JOINTS_3" ->
//                            new AccessorDataInt8(buffer, descriptor.elementType, vertexCount, 0, 0, 0, true, false);
//                        default -> throw new IllegalArgumentException("Unsupported semantic: " + descriptor.semantic);
//                    };
//
//                    final Converter<AccessorData, AccessorData> converter;
//
//                    if (CONVERTERS.containsKey(supplier.getClass())) {
//                        converter = (Converter<AccessorData, AccessorData>) CONVERTERS.get(supplier.getClass()).get(consumer.getClass());
//                    } else {
//                        throw new IllegalArgumentException("Can't find convertor from " + supplier.getClass().getSimpleName() + " to " + consumer.getClass().getSimpleName());
//                    }
//
//
//                    for (int elem = 0; elem < supplier.getElementCount(); elem++) {
//                        for (int comp = 0; comp < supplier.getComponentCount(); comp++) {
//                            converter.convert(supplier, elem, comp, consumer, elem, comp);
//                        }
//                    }
//
//                    final GltfBuffer elementBuffer = new GltfBuffer(file);
//                    elementBuffer.setData(buffer.position(0));
//
//                    elementBuffers.add(elementBuffer);
//
//                    final GltfBufferView elementBufferView = new GltfBufferView(file, elementBuffer);
//                    elementBufferView.byteOffset = 0;
//                    elementBufferView.byteLength = buffer.capacity();
//
//                    final GltfAccessor elementAccessor = new GltfAccessor(file, elementBufferView);
//                    elementAccessor.type = consumer.getElementType().name();
//                    elementAccessor.componentType = consumer.getComponentType().getId();
//                    elementAccessor.normalized = consumer.isNormalized();
//                    elementAccessor.count = vertexCount;
//
//                    meshPrimitives.attributes.put(descriptor.semantic, file.accessors.indexOf(elementAccessor));
//                });
//            }
//
//            GltfBuffer indexBuffer;
//            RTTIObject indexArrayUUID = indexArray.get("ObjectUUID");
//            ComponentType componentType = switch (indices.str("Format")) {
//                case "Index16" -> ComponentType.UNSIGNED_SHORT;
//                case "Index32" -> ComponentType.UNSIGNED_INT;
//                default -> throw new IllegalArgumentException("Unsupported index format: " + indices.str("Format"));
//            };
//
//            if (indexBuffers.containsKey(indexArrayUUID)) {
//                indexBuffer = indexBuffers.get(indexArrayUUID);
//            } else {
//                final var accessor = switch (indices.str("Format")) {
//                    case "Index16" ->
//                        new AccessorDataInt16(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
//                    case "Index32" ->
//                        new AccessorDataInt32(dataSource, ElementType.SCALAR, indexCount, 0, 0, dataSourceOffset, false, false);
//                    default -> throw new IllegalArgumentException("Unsupported index format: " + indices.str("Format"));
//                };
//
//                final var buffer = ByteBuffer
//                    .allocate(accessor.getElementCount() * accessor.getComponentType().getSize())
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//                for (int i = 0; i < indexCount; i++) {
//                    if (accessor instanceof AccessorDataInt16 i16) {
//                        buffer.putShort(i16.get(i, 0));
//                    } else {
//                        buffer.putInt(((AccessorDataInt32) accessor).get(i, 0));
//                    }
//                }
//
//                indexBuffer = new GltfBuffer(file);
//                indexBuffer.setData(buffer.position(0));
//                indexBuffers.put(indexArrayUUID, indexBuffer);
//            }
//
//            final GltfBufferView gltfBufferView = new GltfBufferView(file, indexBuffer);
//            gltfBufferView.byteOffset = indexStartIndex * componentType.getSize();
//            gltfBufferView.byteLength = (indexEndIndex - indexStartIndex) * componentType.getSize();
//
//            final GltfAccessor gltfAccessor = new GltfAccessor(file, gltfBufferView);
//            gltfAccessor.type = "SCALAR";
//            gltfAccessor.componentType = componentType.getId();
//            gltfAccessor.count = indexEndIndex - indexStartIndex;
//
//            meshPrimitives.indices = file.accessors.indexOf(gltfAccessor);
//
//            dataSourceOffset += IOUtils.alignUp(indexBuffer.byteLength, 256);
//        }
//
//
//        return List.of(meshNode);
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

    private interface Converter<SRC_T extends AccessorData, DST_T extends AccessorData> {
        void convert(@NotNull SRC_T src, int strElementIndex, int srcComponentIndex, @NotNull DST_T dst, int dstElementIndex, int dstComponentIndex);
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
